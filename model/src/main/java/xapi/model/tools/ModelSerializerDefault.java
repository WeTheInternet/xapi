/**
 *
 */
package xapi.model.tools;

import xapi.annotation.model.KeyOnly;
import xapi.collect.X_Collect;
import xapi.collect.api.*;
import xapi.collect.proxy.api.CollectionProxy;
import xapi.collect.proxy.impl.MapOf;
import xapi.dev.source.CharBuffer;
import xapi.fu.In2Out1;
import xapi.fu.X_Fu;
import xapi.fu.data.*;
import xapi.fu.itr.SizedIterable;
import xapi.fu.java.X_Jdk;
import xapi.fu.log.Log;
import xapi.inject.X_Inject;
import xapi.log.X_Log;
import xapi.model.X_Model;
import xapi.model.api.*;
import xapi.prop.X_Properties;
import xapi.reflect.X_Reflect;
import xapi.source.lex.CharIterator;
import xapi.debug.X_Debug;
import xapi.source.lex.StringCharIterator;
import xapi.util.api.Digester;
import xapi.util.api.SuccessHandler;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.*;

public class ModelSerializerDefault <M extends Model> implements ModelSerializer<M>{

  private final ClassTo<PrimitiveReader> primitiveReaders;

  private final ClassTo<In2Out1<Class, Class, Object>> collectionFactories;

  public ModelSerializerDefault() {
    this(X_Collect.newClassMap(PrimitiveReader.class));
  }

  public ModelSerializerDefault(final ClassTo<PrimitiveReader> primitiveReaders) {
    this.primitiveReaders = primitiveReaders;
    collectionFactories = X_Collect.newClassMap(In2Out1.class, X_Collect.MUTABLE_INSERTION_ORDERED);
  }

  public static ModelModule deserialize(final CharIterator chars, PrimitiveSerializer primitives) {
    final ModelModule module = new ModelModule();
    module.setUuid(primitives.deserializeString(chars));
    int numStrongNames = primitives.deserializeInt(chars);
    while (numStrongNames --> 0) {
      module.addStrongName(primitives.deserializeString(chars));
    }
    module.setModuleName(primitives.deserializeString(chars));
    primitives = new ClusteringPrimitiveDeserializer(primitives, chars) {
      @Override
      @SuppressWarnings("unchecked")
      public <C> Class<C> deserializeClass(final CharIterator c) {
        // might be able to avoid this eager register() call...
        final Class<C> cls = super.deserializeClass(c);
        if (Model.class.isAssignableFrom(cls)) {
          X_Model.getService().register(Class.class.cast(cls));
        } else if (cls.isArray()) {
          Class<?> component = cls.getComponentType();
          while (component.isArray()) {
            component = cls.getComponentType();
          }
          if (Model.class.isAssignableFrom(component)) {
            X_Model.getService().register(Class.class.cast(component));
          }
        }
        return cls;
      }
    };
    if (!chars.hasNext()) {
        X_Log.warn(ModelModule.class, "Encountered module with no model manifests", module);
        return module;
    }
    int manifests = primitives.deserializeInt(chars);
    while (manifests --> 0) {
      final ModelManifest manifest = ModelManifest.deserialize(chars, primitives);
      module.addManifest(manifest);
    }
    return module;
  }

  public static ModelModule deserialize(final String chars) {
    final StringCharIterator iter = new StringCharIterator(chars);
    return deserialize(iter, X_Inject.instance(PrimitiveSerializer.class));
  }

  public static CharBuffer serialize(final CharBuffer into, final ModelModule module, final PrimitiveSerializer primitives) {
    // Append the uuid, as a string (using a leading size for deserialization purposes)
    final String uuid = toUuid(module);
    into.append(primitives.serializeString(uuid));
    final String[] strongNames = module.getStrongNames();
    into.append(primitives.serializeInt(strongNames.length));
    for (final String strongName : strongNames) {
      into.append(primitives.serializeString(strongName));
    }
    into.append(calculateSerialization(module, primitives));

    return into;
  }

  public static String toUuid(final ModelModule module) {
    String uuid = module.getUuid();
    if (uuid == null) {
      final PrimitiveSerializer primitives = X_Inject.instance(PrimitiveSerializer.class);
    // Compute the checksum of the policy itself.  That checksum will become our UUID,
    // which will then be appended before the policy itself.
      final String result = calculateSerialization(module, primitives);
      final Digester digest = X_Inject.instance(Digester.class);
      byte[] asBytes;
      try {
        asBytes = result.getBytes("UTF-8");
      } catch (final UnsupportedEncodingException e) {
        throw X_Debug.rethrow(e);
      }
      final byte[] bytes = digest.digest(asBytes);
      uuid = digest.toString(bytes);
      module.setUuid(uuid);
    }
    return uuid;
  }

  public static String calculateSerialization(ModelModule module, final PrimitiveSerializer primitives) {
    return module.calculateSerial(primitives, me-> {

      // We will build our policy in our own buffer, so we can safely use it to calculate our strong hash later
      final CharBuffer policy = new CharBuffer();

      policy.append(primitives.serializeString(module.getModuleName()));

      final ClusteringPrimitiveSerializer clusterPrim = new ClusteringPrimitiveSerializer(primitives, policy);
      // Directly append the policy to the result (the string is not wrapped),
      // however, we do append the size of the manifests so we know when to stop deserializing
      final SizedIterable<ModelManifest> all = module.getManifests();
      policy.append(clusterPrim.serializeInt(all.size()));
      for (final ModelManifest manifest : all) {
        // TODO: collect up reused strings like classnames, and append them into a "classpool",
        // to reduce the total size of the policy
        ModelManifest.serialize(policy, manifest, clusterPrim);
      }

      String serialized = policy.toSource();
      return serialized;

    });
  }


  public static String serialize(final ModelModule module) {
    final CharBuffer buffer = new CharBuffer();
    serialize(buffer, module, X_Inject.instance(PrimitiveSerializer.class));
    return buffer.toSource();
  }

  @Override
  public CharBuffer modelToString(final Class<? extends Model> modelType, final M model, final ModelSerializationContext ctx, boolean keyOnly) {
    final CharBuffer out = new CharBuffer();
    ctx.getBuffer().addToEnd(out);
    write(model, out, ctx);
    assert model == null || model.equals(modelFromString(modelType, CharIterator.forString(out.toSource()),
        new ModelDeserializationContext(model, ctx.getService(), ctx.getManifest()), keyOnly)) :
        "Model serialization failure; Bad serialization:\n" + out.toSource()+"\n!=" + model;
    return out;
  }

  protected void write(final M model, final CharBuffer out, final ModelSerializationContext ctx) {
    final PrimitiveSerializer primitives = ctx.getPrimitives();
    writeKey(model, out, ctx);
    if (model == null) {
      System.out.println("hm... null model written... ctx: " + ctx.getBuffer());
    } else {
      for (final String key : ctx.getPropertyNames(model)) {
        if (preventSerialization(model, key, ctx)) {
          continue;
        }
        final Object value = model.getProperty(key);
        final Class<?> propertyType = model.getPropertyType(key);
        writeObject(out, key, propertyType, value, primitives, ctx);
      }
    }
  }

  @Override
  public void writeKey(final M model, final CharBuffer out, final ModelSerializationContext ctx) {
    final PrimitiveSerializer primitives = ctx.getPrimitives();
    if (model == null) {
      out.append(primitives.serializeInt(-2));
      return;
    }
    final ModelKey modelKey = model.getKey();
    if (modelKey == null) {
      out.append(primitives.serializeInt(-1));
    } else {
      final String keyString = ctx.getService().keyToString(modelKey);
      out.append(primitives.serializeInt(0));
      out.append(primitives.serializeString(keyString));
    }
  }

  protected boolean preventSerialization(final M model, final String key, final ModelSerializationContext ctx) {
    if (ctx.getManifest() == null) {
      return false; // No manifest means we will just serialize all fields
    }
    final ModelManifest manifest = ctx.getManifest();
    assert manifest.getMethodData(key) != null : "Invalid manifest; no data found for "+model.getType()+"."+key;
    if (ctx.isClientToServer()) {
      return !manifest.isClientToServerEnabled(key);
    } else {
      return !manifest.isServerToClientEnabled(key);
    }
  }

  protected boolean preventDeserialization(final M model, final String key, final ModelDeserializationContext ctx) {
    if (ctx.getManifest() == null) {
      return false; // No manifest means we will just deserialize all fields
    }
    final ModelManifest manifest = ctx.getManifest();
    assert manifest.getMethodData(key) != null : "Invalid manifest; no data found for "+model.getType()+"."+key;
    if (ctx.isClientToServer()) {
      return !manifest.isServerToClientEnabled(key);
    } else {
      return !manifest.isClientToServerEnabled(key);
    }
  }

  protected boolean isSupportedEnumType(final Class<?> propertyType) {
    return propertyType.isEnum();
  }

  /**
   * This method is abstract so that providers in runtimes with limited / opt-in reflection
   * support (like Gwt) will be able to implement a method that can perform a runtime lookup
   * of a map to see if the given property type is indeed a model.
   */
  protected boolean isModelType(final Class<?> propertyType) {
    return Model.class.isAssignableFrom(propertyType);
  }
  protected boolean isModelKeyType(final Class<?> propertyType) {
    return ModelKey.class.isAssignableFrom(propertyType);
  }
  protected boolean isIterableType(final Class<?> propertyType) {
    return CollectionProxy.class.isAssignableFrom(propertyType);
  }
  protected boolean isStringMapType(final Class<?> propertyType) {
    return StringTo.class.isAssignableFrom(propertyType);
  }
  protected boolean isNumberType(final Class<?> propertyType) {
    return Number.class.isAssignableFrom(propertyType);
  }

  protected void writeArray(final CharBuffer out, final String propName, final Class<?> propertyType, final Object array, final PrimitiveSerializer primitives, final ModelSerializationContext ctx) {
    if (array == null) {
      out.append(primitives.serializeInt(-1));
      return;
    }
    final int len = Array.getLength(array);
    final String length = primitives.serializeInt(len);
    out.append(length);
    final Class<?> childType = propertyType.getComponentType();
    if (childType.isPrimitive()) {
      // For primitives, we will have to serialize those ourselves here, using array reflection
      if (childType == boolean.class) {
        // boolean[] is special.  We want to write those with the serializer
        out.append(primitives.serializeBooleanArray((boolean[])array));
      } else {
        // otherwise, we want to serialize a primitive. We will only bother with int, long, float and double,
        // as all the small int types can just as easily be coerced to int; their size check will already have been done.
        if (childType == double.class) {
          for (int i = 0; i < len; i++) {
            out.append(primitives.serializeDouble(Array.getDouble(array, i)));
          }
        } else if (childType == float.class) {
          for (int i = 0; i < len; i++) {
            out.append(primitives.serializeFloat(Array.getFloat(array, i)));
          }
        } else if (childType == long.class) {
          for (int i = 0; i < len; i++) {
            out.append(primitives.serializeLong(Array.getLong(array, i)));
          }
        } else if (childType == char.class) {
          for (int i = 0; i < len; i++) {
            out.append(primitives.serializeChar(Array.getChar(array, i)));
          }
        } else {
          // all int types
          for (int i = 0; i < len; i++) {
            out.append(primitives.serializeInt(Array.getInt(array, i)));
          }
        }
      }
    } else if (isModelType(childType)) {
      for (int i = 0; i < len; i++) {
        final Object model = Array.get(array, i);
        writeModel(out, propName, childType, (Model)model, primitives, ctx);
      }
    } else if (isModelKeyType(childType)) {
      for (int i = 0; i < len; i++) {
        writeString(out, X_Model.keyToString((ModelKey)Array.get(array, i)), primitives);
      }
    } else if (childType == String.class){
      for (int i = 0; i < len; i++) {
        writeString(out, (String)Array.get(array, i), primitives);
      }
    } else if (isSupportedEnumType(childType)){
      // We are going to assume a homogenous array type...
      assert childType != Enum.class : getClass()+" does not support Enum[] values from model "+propertyType;
      for (int i = 0; i < len; i++) {
        final Enum asEnum = (Enum) Array.get(array, i);
        if (asEnum == null) {
          // Null enum is going to be -1, an impossible ordinal
          out.append(primitives.serializeInt(-1));
        } else {
          out.append(primitives.serializeInt(asEnum.ordinal()));
        }
      }
    } else if (childType.isArray()) {
      // An array of arrays. Oh boy...
      for (int i = 0; i < len; i++) {
        writeArray(out, propName, propertyType.getComponentType(), Array.get(array, i), primitives, ctx);
      }
    } else if (IsEnumerable.class.isAssignableFrom(childType)){
      for (int i = 0; i < len; i++) {
        IsEnumerable item = (IsEnumerable) Array.get(array, i);
        if (item == null) {
          out.append(primitives.serializeClass(childType));
          out.append(primitives.serializeInt(-1));
        } else {
          out.append(primitives.serializeClass(item.getClass()));
          out.append(primitives.serializeInt(item.ordinal()));
        }
      }
    } else if (Duration.class.isAssignableFrom(childType)){
      for (int i = 0; i < len; i++) {
        Duration item = (Duration) Array.get(array, i);
        out.append(primitives.serializeLong(item.getSeconds()));
      }
    } else {
      throw new UnsupportedOperationException("Unable to serialize unsupported array type "+childType);
    }
  }
  protected void writeIterable(final CharBuffer out, final String propName, final CollectionProxy collection, final PrimitiveSerializer primitives, final ModelSerializationContext ctx) {
    if (collection == null) {
      out.append(primitives.serializeInt(-1));
      return;
    }
    final Class keyType = collection.keyType();
    final Class valueType = collection.valueType();

    int len = collection.size();
    if (len == 0 && writeNullForEmpty()) {
      out.append(primitives.serializeInt(-1));
      return;
    }
    final String length = primitives.serializeInt(len);
    out.append(length);
    out.append(primitives.serializeClass(keyType));
    out.append(primitives.serializeClass(valueType));
    if (len == 0) {
      return;
    }
    if (keyType == Integer.class) {
//      integer collection.  If it is dense, we can just write the length and then the items.
      if (collection.readWhileTrue(new In2Out1() {
         int was;
         @Override
         public Boolean io(Object key, Object value) {
           final Integer k = (Integer) key;
           if (++was==k) {
             return true;
           }
           return false;
         }
       })) {
        // It is a dense array.  We can write out the values
        out.append(primitives.serializeBoolean(true));
        collection.readWhileTrue((key, value)-> {
             writeObject(out, String.valueOf(key), valueType, value, primitives, ctx);
             return true;
           });
      } else {
        // it is a sparse array. write out w/ nulls
        out.append(primitives.serializeBoolean(false));
        collection.readWhileTrue((key, value) -> {
                out.append(primitives.serializeInt((Integer) key));
                writeObject(out, propName, valueType, value, primitives, ctx);
                return true;
            }
        );
      }
    } else if (keyType == Class.class) {
        collection.readWhileTrue((key, value) -> {
             out.append(primitives.serializeClass((Class) key));
             writeObject(out, propName, valueType, value, primitives, ctx);
             return true;
         });
    } else if (keyType == String.class) {
        collection.readWhileTrue((key, value) -> {
             out.append(primitives.serializeString((String) key));
             writeObject(out, propName, valueType, value, primitives, ctx);
             return true;
         });
    } else if (keyType == Duration.class) {
        collection.readWhileTrue((key, value) -> {
             final long time = ((Duration)key).getSeconds();
             out.append(primitives.serializeLong(time));
             writeObject(out, propName, valueType, value, primitives, ctx);
             return true;
         });
    } else if (keyType == ModelKey.class) {
        collection.readWhileTrue((key, value) -> {
             final ModelKey k = (ModelKey) key;
             writeString(out, X_Model.keyToString(k), primitives);
             writeObject(out, propName, valueType, value, primitives, ctx);
             return true;
         });
    } else if (IsEnumerable.class.isAssignableFrom(keyType)) {
        collection.readWhileTrue((key, value) -> {
            IsEnumerable k = (IsEnumerable) key;
            out.append(primitives.serializeClass(k.getClass()));
            out.append(primitives.serializeInt(k.ordinal()));
            writeObject(out, propName, valueType, value, primitives, ctx);
            return true;
         });
    } else if (keyType.isEnum()) {
        collection.readWhileTrue((key, value) -> {
            out.append(primitives.serializeString(((Enum) key).name()));
            writeObject(out, propName, valueType, value, primitives, ctx);
            return true;
         });
    } else {
      throw new IllegalStateException("Unsupported key type "+keyType+" in model serializer: "+getClass());
    }
  }

  private boolean writeNullForEmpty() {
    return "true".equals(X_Properties.getProperty("xapiModelNullForEmpty"));
  }

  protected void writeStringMap(final CharBuffer out, final StringTo<?> collection, final PrimitiveSerializer primitives, final ModelSerializationContext ctx) {
    if (collection == null) {
      // For null, we will check for class void and immediately return null.
      out.append(primitives.serializeClass(void.class));
      return;
    }
    final Class valueType = collection.valueType();

    out.append(primitives.serializeClass(valueType));
    int len = collection.size();
    final String length = primitives.serializeInt(len);
    out.append(length);
    if (len == 0) {
      return;
    }
    collection.forBoth((key, value) -> {
         out.append(primitives.serializeString(key));
         writeObject(out, key, valueType, value, primitives, ctx);
     });
  }


  protected Object readIterable(
      Class propertyType,
      String propName,
      CharIterator src,
      PrimitiveSerializer primitives,
      ModelDeserializationContext ctx
  ) {

    int length = primitives.deserializeInt(src);
    if (length == -1) {
      // We are null
      return null;
    }
    if (length == 0 && writeNullForEmpty()) {
      return null;
    }

    final Class keyType = primitives.deserializeClass(src);
    final Class valueType = primitives.deserializeClass(src);
    CollectionProxy result = newResult(propertyType, keyType, valueType);
    if (length == 0) {
      return result;
    }

    if (keyType == Integer.class) {
      boolean dense = primitives.deserializeBoolean(src);
      if (dense) {
        // We can just push onto the array
        for (int i = 0; i < length; i++) {
          Object value = readObject(valueType, propName, src, primitives, ctx);
          result.setValue(new Integer(i), value);
        }
      } else {
        // we need to actually read the keys and set as appropriate
        for (int i = 0; i < length; i++) {
          int key = primitives.deserializeInt(src);
          Object value = readObject(valueType, propName, src, primitives, ctx);
          result.setValue(key, value);
        }
      }
    } else if (keyType == Class.class) {
        for (int i = 0; i < length; i++) {
          Class key = primitives.deserializeClass(src);
          Object value = readObject(valueType, propName, src, primitives, ctx);
          result.setValue(key, value);
        }
    } else if (keyType == String.class) {
        for (int i = 0; i < length; i++) {
          String key = primitives.deserializeString(src);
          Object value = readObject(valueType, propName, src, primitives, ctx);
          result.setValue(key, value);
        }
    } else if (keyType == ModelKey.class) {
        for (int i = 0; i < length; i++) {
          String key = primitives.deserializeString(src);
          final ModelKey k = X_Model.keyFromString(key);
          Object value = readObject(valueType, propName, src, primitives, ctx);
          result.setValue(k, value);
        }
    } else if (IsEnumerable.class.isAssignableFrom(keyType)) {
        for (int i = 0; i < length; i++) {
          Class type = primitives.deserializeClass(src);
          int ordinal = primitives.deserializeInt(src);
          Object value = readObject(valueType, propName, src, primitives, ctx);
          final Object enumKey = type.getEnumConstants()[ordinal];
          result.setValue(enumKey, value);
        }
    } else if (keyType.isEnum()) {
        for (int i = 0; i < length; i++) {
          String key = primitives.deserializeString(src);
          Object value = readObject(valueType, propName, src, primitives, ctx);
          final Enum enumKey = Enum.valueOf(keyType, key);
          result.setValue(enumKey, value);
        }
    } else {
      throw new IllegalStateException("Unsupported key type "+keyType+" in model serializer: "+getClass());
    }

    return result;
  }
  protected Object readStringMap(
      Class propertyType,
      String propName,
      CharIterator src,
      PrimitiveSerializer primitives,
      ModelDeserializationContext ctx
  ) {

    final Class<?> valueType = primitives.deserializeClass(src);
    if (valueType.getName().equals("void")) {
      return null;
    }
    final StringTo<Object> map = X_Collect.newStringMap(valueType);

    int length = primitives.deserializeInt(src);
    if (length == -1) {
      // We are null
      return null;
    }
    if (length == 0) {
      return map;
    }

    for (int i = 0; i < length; i++) {
      String key = readString(src, primitives);
      Object value = readObject(valueType, propName, src, primitives, ctx);
      map.put(key, value);
    }

    return map;
  }

  protected CollectionProxy newResult(Class collectionType, Class keyType, Class<?> valueType) {
    if (collectionFactories.isEmpty()) {
      initializeCollectionFactories(collectionFactories);
    }
    final In2Out1<Class, Class, Object> factory = collectionFactories.get(collectionType);
    return (CollectionProxy) factory.io(keyType, valueType);
  }

  protected void initializeCollectionFactories(ClassTo<In2Out1<Class,Class,Object>> factories) {
    // TODO use whole-world compiler knowledge to erase factories we will never use, as this likely sucks in a lot of code.
    // This would likely be best done in a ModelSerializerGwt that is generated and injected in place of this serializer
    factories.put(IntTo.class, (key, value) -> X_Collect.newList(value));
    factories.put(StringTo.class, (key, value) -> {
      if (value == StringTo.class) {
        return X_Collect.newStringDeepMap(value);
      }
      return X_Collect.newStringMap(value);
    });
    factories.put(MapLike.class, (key, value) -> X_Jdk.mapOrderedInsertion());
    factories.put(ListLike.class, (key, value) -> X_Jdk.list());
    factories.put(SetLike.class, (key, value) -> X_Jdk.setLinked());
    factories.put(MultiSet.class, (key, value) -> X_Jdk.multiSet());
    factories.put(MultiList.class, (key, value) -> X_Jdk.multiListOrderedInsertion());
    factories.put(MapOf.class, (key, value) -> new MapOf(newMap(key, value), key, value));
    factories.put(ObjectTo.class, (key, value) -> X_Collect.newInsertionOrderedMap(key, value));
    factories.put(ClassTo.class, (key, value) -> X_Collect.newClassMap(value, X_Collect.MUTABLE_INSERTION_ORDERED));
    factories.put(IntTo.Many.class, (key, value) -> X_Collect.newIntMultiMap(value));
    factories.put(StringTo.Many.class, (key, value) -> X_Collect.newStringMultiMap(value));
    factories.put(ObjectTo.Many.class, (key, value) -> X_Collect.newMultiMap(key, value));
    factories.put(ClassTo.Many.class, (key, value) -> X_Collect.newClassMultiMap(value));
  }

  protected Map newMap(Class key, Class value) {
    return new LinkedHashMap();
  }

  protected void writeObject(
      CharBuffer out,
      String propName,
      Class valueType,
      Object value,
      PrimitiveSerializer primitives,
      ModelSerializationContext ctx
  ) {
    if (valueType.isArray()) {
      // write an array
      writeArray(out, propName, valueType, value, primitives, ctx);
    } else if (valueType == String.class) {
      final String asString = (String) value;
      writeString(out, asString, primitives);
    } else if (valueType.isPrimitive()) {
      // write a primitive
      if (valueType == double.class) {
        Double asDouble = (Double) value;
        if (asDouble == null) {
          asDouble = 0.;
        }
        out.append(primitives.serializeDouble(asDouble.doubleValue()));
      } else if (valueType == float.class) {
        Float asFloat = (Float) value;
        if (asFloat == null) {
          asFloat = 0f;
        }
        out.append(primitives.serializeFloat(asFloat.floatValue()));
      } else if (valueType == boolean.class) {
        Boolean asBoolean = (Boolean) value;
        if (asBoolean == null) {
          asBoolean = false;
        }
        out.append(primitives.serializeBoolean(asBoolean.booleanValue()));
      } else if (valueType == char.class) {
        Character asCharacter = (Character) value;
        if (asCharacter == null) {
          asCharacter = '0';
        }
        out.append(primitives.serializeChar(asCharacter.charValue()));
      } else if (valueType == long.class) {
        Long asLong  = (Long) value;
        if (asLong == null) {
          asLong = 0L;
        }
        out.append(primitives.serializeLong(asLong.longValue()));
      } else {
        Number asNumber  = (Number) value;
        if (asNumber == null) {
          asNumber = 0;
        }
        // all int types
        out.append(primitives.serializeInt(asNumber.intValue()));
      }
    } else if (Number.class.isAssignableFrom(valueType)) {
      Number nonNullNumber = value == null ? 0 : (Number)value;
      if (valueType == Float.class || valueType == Double.class || valueType == BigDecimal.class) {
        out.append(primitives.serializeDouble(nonNullNumber.doubleValue()));
      } else {
        out.append(primitives.serializeLong(nonNullNumber.longValue()));
      }
    } else if (isModelType(valueType)) {
      writeModel(out, propName, valueType, (Model)value, primitives, ctx);
    } else if (isModelKeyType(valueType)) {
      writeString(out, X_Model.keyToString((ModelKey)value), primitives);
    } else if (isIterableType(valueType)) {
      writeIterable(out, propName, (CollectionProxy)value, primitives, ctx);
      // Figure out how to support either ComponentList, Allable, or some other not-CollectionProxy collection.
    } else if (isStringMapType(valueType)) {
      writeStringMap(out, (StringTo)value, primitives, ctx);
    } else if (valueType == Class.class) {
      out.append(primitives.serializeClass((Class) value));
    } else if (Duration.class.isAssignableFrom(valueType)) {
      Long asLong  = ((Duration) value).getSeconds();
      if (asLong == null) {
        asLong = 0L;
      }
      out.append(primitives.serializeLong(asLong.longValue()));
    } else if (isSupportedEnumType(valueType)) {
      if (value == null) {
        out.append(primitives.serializeInt(-1));
      } else {
        final Enum asEnum = (Enum) value;
        out.append(primitives.serializeInt(asEnum.ordinal()));
      }
    } else if (EnumSet.class.isAssignableFrom(valueType)) {
      if (value == null) {
        out.append(primitives.serializeInt(-1));
        return;
      }
      EnumSet<? extends Enum<?>> item = (EnumSet<? extends Enum<?>>) value;
      int size = item.size();
      int cnt = 0;
      out.append(primitives.serializeInt(size));
      for (Enum<? extends Enum<?>> i : item) {
        if (cnt == 0) {
          // the "enum type" is the supertype of each instance of the enum type
          out.append(primitives.serializeClass(i.getDeclaringClass()));
        }
        cnt++;
        serializeEnum(out, primitives, i);
      }
    } else if (EnumMap.class.isAssignableFrom(valueType)) {
      if (value == null) {
        out.append(primitives.serializeInt(-1));
        return;
      }
      EnumMap<? extends Enum<?>, ?> item = (EnumMap) value;
      int size = item.size();
      if (size == 0) {
          // we can't serialize the enum map's type w/o any items to look at.
          // convert to null.
          out.append(primitives.serializeInt(-1));
          return;
      }
      int cnt = 0;
      out.append(primitives.serializeInt(size));
      for (Map.Entry<? extends Enum<?>, ?> e : item.entrySet()) {
        Enum itemType = e.getKey();
        if (cnt == 0) {
          // the "enum type" is the declaring class of each instance of the enum type
          out.append(primitives.serializeClass(itemType.getDeclaringClass()));
        }
        Object itemValue = e.getValue();
        serializeEnum(out, primitives, itemType);
        out.append(primitives.serializeClass(itemValue.getClass()));
        String fakeName = propName + "_" + cnt++;
        writeObject(out, fakeName, itemValue.getClass(), itemValue, primitives, ctx);
      }
    } else if (IsEnumerable.class.isAssignableFrom(valueType)) {
      IsEnumerable item = (IsEnumerable) value;
      if (item == null) {
        out.append(primitives.serializeClass(valueType));
        out.append(primitives.serializeInt(-1));
      } else {
        out.append(primitives.serializeClass(item.getClass()));
        out.append(primitives.serializeInt(item.ordinal()));
      }
    } else {
      throw new IllegalStateException("Unserializable field type: " + propName + " (" + valueType + ")");
    }
  }

  protected void writeString(final CharBuffer out, final String string, final PrimitiveSerializer primitives) {
    if (string == null) {
      out.append(primitives.serializeInt(-1));
    } else {
      out.append(primitives.serializeInt(string.length()));
      out.append(string);
    }
  }

  @SuppressWarnings({
      "rawtypes", "unchecked"
  })
  protected void writeModel(final CharBuffer out, final String propName, final Class<?> propertyType, final Model childModel, final PrimitiveSerializer primitives, final ModelSerializationContext ctx) {
    final KeyOnly keyOnlyAnno = propertyType.getAnnotation(KeyOnly.class);
    boolean keyOnly = keyOnlyAnno != null;
    boolean autoSave = keyOnly ? keyOnlyAnno.autoSave() : false;
    if (!keyOnly && ctx.getManifest() != null) {
      final ModelManifest manifest = ctx.getManifest();
      keyOnly = manifest.isKeyOnly() || manifest.isKeyOnly(propName);
    }
    final ModelSerializer serializer = newSerializer(Class.class.cast(propertyType), ctx);
    final CharBuffer was = ctx.getBuffer();
    ctx.setBuffer(out);
    try {
      if (keyOnly) {
          if (autoSave) {
            // also independently save this child model
            ctx.getService().persist(childModel, SuccessHandler.handler(
                    succeed -> {
                      // nothing to do here... yikes. Need to route asynchronicity in better
                      X_Log.info(ModelSerializerDefault.class, "Auto-saved child model " + succeed.getKey() + " in parent field " + propName);
                    },
                    fail -> {
                        X_Log.error(ModelSerializerDefault.class, "UNABLE TO AUTOSAVE CHILD MODEL.\n",
                                "parent field name: " + propName + " ; model: " + childModel);
                    }
            ));
          }
          serializer.writeKey(childModel, out, ctx);
      } else {
          serializer.modelToString(propertyType, childModel, ctx, false);
      }
    } finally {
      ctx.setBuffer(was);
    }
  }

  protected <Mod extends Model> ModelSerializer<Mod> newSerializer(final Class<Mod> propertyType, final ModelSerializationContext ctx) {
    return new ModelSerializerDefault<Mod>(primitiveReaders);
  }

  @Override
  @SuppressWarnings("unchecked")
  public M modelFromString(final Class<? extends Model> modelType, final CharIterator src, final ModelDeserializationContext ctx, boolean keyOnly) {
    final PrimitiveSerializer primitives = ctx.getPrimitives();
    final int modelState = primitives.deserializeInt(src);
    if (modelState == -2) {
      return null;
    }
    ModelKey key;
    if (modelState != -1) {
      // There is a key for this model
//      final String keyString = src.consume(modelState).toString();
      final String keyString = primitives.deserializeString(src);
      key = ctx.getService().keyFromString(keyString);
    } else {
      key = null;
    }
    final M model = (M) ctx.getModel();
    assert model != null : "Null model found "+src;
    model.setKey(key);
    if (ctx.isSubModel() && keyOnly) {
      // a keyOnly type that's a sub-model won't have any field info in our model source, load that in bg.
      final Class<Model> actualType = ctx.getService().typeToClass(key.getKind());
      if (actualType != modelType) {
        X_Log.debug(ModelSerializerDefault.class, "Field model requested type ", modelType, " but actual model was of type ", actualType);
      }
      ctx.getService().load(actualType, key, SuccessHandler.handler(
              win -> {
                // can't use model.absorb, it barfs when we send a proxy through lambdas (can't cast proxy to Model anymore...)
                for (Map.Entry<String, Object> property : win.getProperties()) {
                  model.setProperty(property.getKey(), property.getValue());
                }
                // TODO: we need to contribute these to a promise-like object...
                // or at least a Model.getBlockers() method of some kind, to allow threaded enviros to block.
                X_Log.debug(ModelSerializerDefault.class, "Finished loading sub-model ", win.getKey());
              }, lose -> {
                X_Log.error(ModelSerializerDefault.class, "Error loading sub-model ", key, lose);
              }
      ));
      return model;
    }
    final String[] propNames = ctx.getPropertyNames(model);
    for (final String propertyName : propNames) {
      if (preventDeserialization(model, propertyName, ctx)) {
        continue;
      }
      try {
          readProperty(model, propertyName, src, ctx);
      } catch (Throwable t) {
        Log.loggerFor(ModelSerializerDefault.class, this)
                .log(ModelSerializerDefault.class, Log.LogLevel.ERROR, "Unable to read property ",
                        propertyName, " into model " + model + "\n\nFull key list:", propNames, "\nRemaining source:\n", src.consumeAll());
          throw t;
      }
    }
    return model;
  }

  protected void readProperty(final Model model, final String propertyName, final CharIterator src, final ModelDeserializationContext ctx) {
    final Class<?> propertyType = model.getPropertyType(propertyName);
    final PrimitiveSerializer primitives = ctx.getPrimitives();
    if (propertyType.isArray()) {
      model.setProperty(propertyName, readArray(propertyType.getComponentType(), propertyName, src, primitives, ctx));
    } else if (propertyType.isPrimitive()) {
      final Object value = readPrimitive(propertyType, src, primitives);
      model.setProperty(propertyName, value);
    } else {
      Object value = readObject(propertyType, propertyName, src, primitives, ctx);
      model.setProperty(propertyName, value);
    }
  }

  /**
   * @param componentType
   * @param propName
   * @param src
   * @param primitives
   * @param ctx
   * @return
   */
  protected Object readArray(final Class<?> componentType, final String propName, final CharIterator src, final PrimitiveSerializer primitives,
      final ModelDeserializationContext ctx) {
    final int len = primitives.deserializeInt(src);
    if (len == -1) {
      return null;
    }
    if (componentType.isPrimitive()) {
      final Object array = Array.newInstance(componentType, len);
      for (int i = 0;i < len;i++) {
        if (componentType == int.class) {
          Array.setInt(array, i, primitives.deserializeInt(src));
        } else if (componentType == float.class) {
          Array.setFloat(array, i, primitives.deserializeFloat(src));
        } else if (componentType == boolean.class) {
          Array.setBoolean(array, i, primitives.deserializeBoolean(src));
        } else if (componentType == char.class) {
          Array.setChar(array, i, primitives.deserializeChar(src));
        } else if (componentType == double.class) {
          Array.setDouble(array, i, primitives.deserializeDouble(src));
        } else if (componentType == long.class) {
          Array.setLong(array, i, primitives.deserializeLong(src));
        } else if (componentType == short.class) {
          Array.setShort(array, i, primitives.deserializeShort(src));
        } else if (componentType == byte.class) {
          Array.setByte(array, i, primitives.deserializeByte(src));
        } else {
          throw new UnsupportedOperationException("Unsupported array component type"+componentType);
        }
      }
      return array;
    } else {
      final Object array = Array.newInstance(componentType, len);
      for (int i = 0;i < len;i++) {
        final Object value = readObject(componentType, propName, src, primitives, ctx);
        Array.set(array, i, value);
      }
      return array;
    }
  }

  /**
   * @param componentType
   * @param src
   * @param primitives
   * @return
   */
  protected Object readPrimitive(final Class<?> componentType, final CharIterator src, final PrimitiveSerializer primitives) {
    final PrimitiveReader reader = getPrimitiveReader(componentType, primitiveReaders);
    return reader.readPrimitive(componentType, src, primitives);
  }

  protected String readString(final CharIterator src, final PrimitiveSerializer primitives) {
    final int len = primitives.deserializeInt(src);
    if (len == -1) {
      return null;
    }
    return String.valueOf(src.consume(len));
  }

  protected long readLong(final CharIterator src, final PrimitiveSerializer primitives) {
    return primitives.deserializeLong(src);
  }

  protected PrimitiveReader getPrimitiveReader(final Class<?> componentType, final ClassTo<PrimitiveReader> primitiveReaders) {
    PrimitiveReader reader = primitiveReaders.get(componentType);
    if (reader == null) {
      if (componentType == int.class) {
        reader = PrimitiveReaders.forInt();
      } else if (componentType == float.class) {
        reader = PrimitiveReaders.forFloat();
      } else if (componentType == boolean.class) {
        reader = PrimitiveReaders.forBoolean();
      } else if (componentType == char.class) {
        reader = PrimitiveReaders.forChar();
      } else if (componentType == double.class) {
        reader = PrimitiveReaders.forDouble();
      } else if (componentType == long.class) {
        reader = PrimitiveReaders.forLong();
      } else if (componentType == short.class) {
        reader = PrimitiveReaders.forShort();
      } else if (componentType == byte.class) {
        reader = PrimitiveReaders.forByte();
      } else {
        throw new UnsupportedOperationException("Unsupported primitive type "+componentType);
      }
      primitiveReaders.put(componentType, reader);
    }
    return reader;
  }

  /**
   * @param propertyType
   * @param propName
   * @param src
   * @param ctx
   * @param primitives
   * @return
   */
  @SuppressWarnings({
      "unchecked", "rawtypes"
  })
  protected Object readObject(final Class propertyType, final String propName, final CharIterator src, final PrimitiveSerializer primitives, final ModelDeserializationContext ctx) {
    if (propertyType == String.class) {
      return readString(src, primitives);
    } else if (isModelType(propertyType)) {
      // We have an inner model to read!
      final ModelDeserializationContext context = ctx.createChildContext(propertyType);
      boolean keyOnly = ctx.isKeyOnly();
      if (!keyOnly) {
        final ModelManifest manifest = ctx.getManifest();
        if (manifest != null) {
          keyOnly = manifest.isKeyOnly() || manifest.isKeyOnly(propName);
        }
        if (propertyType.getAnnotation(KeyOnly.class) != null) {
          keyOnly = true;
        }
      }
      return modelFromString(propertyType, src, context, keyOnly);
    } else if (isModelKeyType(propertyType)) {
      String key = readString(src, primitives);
      return X_Model.keyFromString(key);
    } else if (Number.class.isAssignableFrom(propertyType)) {
      if (propertyType == Integer.class) {
        return (int) primitives.deserializeLong(src);
      } else if (propertyType == Long.class) {
        return primitives.deserializeLong(src);
      } else if (propertyType == Float.class) {
        return (float)primitives.deserializeDouble(src);
      } else if (propertyType == Double.class) {
        return primitives.deserializeDouble(src);
      } else if (propertyType == Short.class) {
        return (short)primitives.deserializeLong(src);
      } else if (propertyType == Byte.class) {
        return (byte)primitives.deserializeLong(src);
      } else if (propertyType == BigInteger.class) {
        // need to do something... less bad here
        return BigInteger.valueOf((long)primitives.deserializeLong(src));
      } else if (propertyType == BigDecimal.class) {
        // need to do something... less bad here
        return BigDecimal.valueOf((double)primitives.deserializeDouble(src));
      }

    } else if (propertyType.isArray()) {
      return readArray(propertyType.getComponentType(), propName, src, primitives, ctx);
    } else if (isIterableType(propertyType)) {
      return readIterable(propertyType, propName, src, primitives, ctx);
    } else if (isStringMapType(propertyType)) {
      return readStringMap(propertyType, propName, src, primitives, ctx);
    } else if (propertyType.isEnum()) {
      // No great way to deserialize enums without reflection, so lets leave a hook for environments
      // where reflection is not possible or preferable can implement a mapping of enum class to enum values[]...
      return readEnum(propertyType, src, primitives, ctx);
    } else if (propertyType == Class.class) {
      return primitives.deserializeClass(src);
    } else if (EnumSet.class.isAssignableFrom(propertyType)) {
      int amt = primitives.deserializeInt(src);
      Class<? extends Enum<?>> enumType = primitives.deserializeClass(src);
      if (amt == -1) {
        return null;
      }
      if (amt == 0) {
        return EnumSet.noneOf((Class)enumType);
      }
      List<Enum> items = new ArrayList<>(amt);
      while (amt --> 0) {
        items.add(deserializeEnum(enumType, primitives, src));
      }
      // pull the last item off list. this is cheapest for array list
      Enum last = items.remove(items.size() - 1);
      // now, send it all to EnumSet...
//      final Enum[] array = items.toArray(X_Reflect.newArray(enumType, items.size()));
      final Enum[] arr = (Enum[]) X_Fu.newArray(enumType, items.size());
      final Enum[] array = items.toArray(arr);
      return EnumSet.of(last, array);
    } else if (EnumMap.class.isAssignableFrom(propertyType)) {
      int amt = primitives.deserializeInt(src);
      if (amt == -1) {
        return null;
      }
      Class<? extends Enum<?>> enumType = primitives.deserializeClass(src);
      EnumMap map = new EnumMap(enumType);
      while (amt --> 0) {
        final Enum key = deserializeEnum(enumType, primitives, src);
        final Class valType = primitives.deserializeClass(src);
        String fakeName = propName + "_" + key.ordinal();
        final Object value = readObject(valType, fakeName, src, primitives, ctx);
        map.put(key, value);
      }
      return map;
    } else if (IsEnumerable.class.isAssignableFrom(propertyType)) {
      final Class cls = primitives.deserializeClass(src);
      final int ordinal = primitives.deserializeInt(src);
      if (ordinal == -1) {
        return null;
      }
      return cls.getEnumConstants()[ordinal];
    } else if (Duration.class.isAssignableFrom(propertyType)) {
      final long seconds = readLong(src, primitives);
      return Duration.ofSeconds(seconds);
    }
    throw new UnsupportedOperationException("Unable to deserialize object of type "+propertyType);
  }

  protected void serializeEnum(final CharBuffer out, final PrimitiveSerializer primitives, final Enum<? extends Enum<?>> i) {
    final String serialized = primitives.serializeInt(i.ordinal());
    out.append(serialized);
  }
  protected Enum deserializeEnum(final Class<? extends Enum<?>> enumType, final PrimitiveSerializer primitives, final CharIterator src) {
      int ordinal = primitives.deserializeInt(src);
      return enumType.getEnumConstants()[ordinal];
  }

  /**
   * @param propertyType
   * @param src
   * @param primitives
   * @param ctx
   * @return
   */
  @SuppressWarnings("unchecked")
  protected Object readEnum(final Class propertyType, final CharIterator src, final PrimitiveSerializer primitives,
      final ModelDeserializationContext ctx) {
    try {
      final int ordinal = primitives.deserializeInt(src);
      if (ordinal == -1) {
        return null;
      }
      final Method method = propertyType.getMethod("values");
      final Object values = method.invoke(null);
      return Array.get(values, ordinal);
    } catch (final Throwable e) {
      X_Log.error(getClass(), "Error reading enum "+propertyType+" from "+src);
      throw X_Debug.rethrow(e);
    }
  }

}
