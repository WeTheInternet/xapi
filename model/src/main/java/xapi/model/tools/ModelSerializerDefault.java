/**
 *
 */
package xapi.model.tools;

import xapi.collect.X_Collect;
import xapi.collect.api.ClassTo;
import xapi.collect.api.IntTo;
import xapi.collect.api.ObjectTo;
import xapi.collect.api.StringTo;
import xapi.collect.proxy.api.CollectionProxy;
import xapi.collect.proxy.impl.MapOf;
import xapi.dev.source.CharBuffer;
import xapi.fu.In2Out1;
import xapi.fu.itr.SizedIterable;
import xapi.inject.X_Inject;
import xapi.log.X_Log;
import xapi.model.X_Model;
import xapi.model.api.*;
import xapi.source.lex.CharIterator;
import xapi.debug.X_Debug;
import xapi.source.lex.StringCharIterator;
import xapi.util.api.Digester;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

public class ModelSerializerDefault <M extends Model> implements ModelSerializer<M>{

  private final ClassTo<PrimitiveReader> primitiveReaders;

  private final ClassTo<In2Out1<Class, Class, Object>> collectionFactories;

  public ModelSerializerDefault() {
    this(X_Collect.newClassMap(PrimitiveReader.class));
  }

  public ModelSerializerDefault(final ClassTo<PrimitiveReader> primitiveReaders) {
    this.primitiveReaders = primitiveReaders;
    collectionFactories = X_Collect.newClassMap(In2Out1.class);
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
  public CharBuffer modelToString(final M model, final ModelSerializationContext ctx) {
    final CharBuffer out = new CharBuffer();
    ctx.getBuffer().addToEnd(out);
    write(model, out, ctx);
    assert model == null || model.equals(modelFromString(CharIterator.forString(out.toSource()),
        new ModelDeserializationContext(model, ctx.getService(), ctx.getManifest()))) :
        "Model serialization failure; Bad serialization:\n" + out.toSource()+"\n!=" + model;
    return out;
  }

  protected void write(final M model, final CharBuffer out, final ModelSerializationContext ctx) {
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
      out.append(primitives.serializeString(keyString));
    }
    for (final String key : model.getPropertyNames()) {
      if (preventSerialization(model, key, ctx)) {
        continue;
      }
      final Object value = model.getProperty(key);
      final Class<?> propertyType = model.getPropertyType(key);
      writeObject(out, propertyType, value, primitives, ctx);
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

  protected void writeArray(final CharBuffer out, final Class<?> propertyType, final Object array, final PrimitiveSerializer primitives, final ModelSerializationContext ctx) {
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
        writeModel(out, childType, (Model)model, primitives, ctx);
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
        writeArray(out, propertyType.getComponentType(), Array.get(array, i), primitives, ctx);
      }
    } else {
      throw new UnsupportedOperationException("Unable to serialize unsupported array type "+childType);
    }
  }
  protected void writeIterable(final CharBuffer out, final CollectionProxy collection, final PrimitiveSerializer primitives, final ModelSerializationContext ctx) {
    if (collection == null) {
      out.append(primitives.serializeClass(String.class));
      out.append(primitives.serializeClass(String.class));
      out.append(primitives.serializeInt(-1));
      return;
    }
    final Class keyType = collection.keyType();
    final Class valueType = collection.valueType();

    out.append(primitives.serializeClass(keyType));
    out.append(primitives.serializeClass(valueType));
    int len = collection.size();
    final String length = primitives.serializeInt(len);
    out.append(length);
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
             writeObject(out, valueType, value, primitives, ctx);
             return true;
           });
      } else {
        // it is a sparse array. write out w/ nulls
        out.append(primitives.serializeBoolean(false));
        collection.readWhileTrue((key, value) -> {
                out.append(primitives.serializeInt((Integer) key));
                writeObject(out, valueType, value, primitives, ctx);
                return true;
            }
        );
      }
    } else if (keyType == Class.class) {
        collection.readWhileTrue((key, value) -> {
             out.append(primitives.serializeClass((Class) key));
             writeObject(out, valueType, value, primitives, ctx);
             return true;
         });
    } else if (keyType == String.class) {
        collection.readWhileTrue((key, value) -> {
             out.append(primitives.serializeString((String) key));
             writeObject(out, valueType, value, primitives, ctx);
             return true;
         });
    } else if (keyType.isEnum()) {
        collection.readWhileTrue((key, value) -> {
             out.append(primitives.serializeString(((Enum) key).name()));
             writeObject(out, valueType, value, primitives, ctx);
             return true;
         });
    } else {
      assert false : "Unsupported key type "+keyType+" in model serializer: "+getClass();
    }
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
         writeObject(out, valueType, value, primitives, ctx);
     });
  }


  protected Object readIterable(
      Class propertyType,
      CharIterator src,
      PrimitiveSerializer primitives,
      ModelDeserializationContext ctx
  ) {

    final Class keyType = primitives.deserializeClass(src);
    final Class valueType = primitives.deserializeClass(src);

    int length = primitives.deserializeInt(src);
    if (length == -1) {
      // We are null
      return null; // TODO: consider automatic never-nullness?
    }
    CollectionProxy result = newResult(propertyType, keyType, valueType);
    if (length == 0) {
      return result;
    }

    if (keyType == Integer.class) {
      boolean dense = primitives.deserializeBoolean(src);
      if (dense) {
        // We can just push onto the array
        for (int i = 0; i < length; i++) {
          Object value = readObject(valueType, src, primitives, ctx);
          result.setValue(new Integer(i), value);
        }
      } else {
        // we need to actually read the keys and set as appropriate
        for (int i = 0; i < length; i++) {
          int key = primitives.deserializeInt(src);
          Object value = readObject(valueType, src, primitives, ctx);
          result.setValue(key, value);
        }
      }
    } else if (keyType == Class.class) {
        for (int i = 0; i < length; i++) {
          Class key = primitives.deserializeClass(src);
          Object value = readObject(valueType, src, primitives, ctx);
          result.setValue(key, value);
        }
    } else if (keyType == String.class) {
        for (int i = 0; i < length; i++) {
          String key = primitives.deserializeString(src);
          Object value = readObject(valueType, src, primitives, ctx);
          result.setValue(key, value);
        }
    } else if (keyType.isEnum()) {
        for (int i = 0; i < length; i++) {
          String key = primitives.deserializeString(src);
          Object value = readObject(valueType, src, primitives, ctx);
          final Enum enumKey = Enum.valueOf(valueType, key);
          result.setValue(enumKey, value);
        }
    } else {
      assert false : "Unsupported key type "+keyType+" in model serializer: "+getClass();
    }

    return result;
  }
  protected Object readStringMap(
      Class propertyType,
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
      Object value = readObject(valueType, src, primitives, ctx);
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
    factories.put(MapOf.class, (key, value) -> new MapOf(newMap(key, value), key, value));
    factories.put(ObjectTo.class, (key, value) -> X_Collect.newMap(key, value));
    factories.put(ClassTo.class, (key, value) -> X_Collect.newClassMap(value));
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
      Class valueType,
      Object value,
      PrimitiveSerializer primitives,
      ModelSerializationContext ctx
  ) {
    if (valueType.isArray()) {
      // write an array
      writeArray(out, valueType, value, primitives, ctx);
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
    } else if (isModelType(valueType)) {
      writeModel(out, valueType, (Model)value, primitives, ctx);
    } else if (isModelKeyType(valueType)) {
      writeString(out, X_Model.keyToString((ModelKey)value), primitives);
    } else if (isIterableType(valueType)) {
      writeIterable(out, (CollectionProxy)value, primitives, ctx);
      // Figure out how to support either ComponentList, Allable, or some other not-CollectionProxy collection.
    } else if (isStringMapType(valueType)) {
      writeStringMap(out, (StringTo)value, primitives, ctx);
    } else if (isSupportedEnumType(valueType)) {
      if (value == null) {
        out.append(primitives.serializeInt(-1));
      } else {
        final Enum asEnum = (Enum) value;
        out.append(primitives.serializeInt(asEnum.ordinal()));
      }
    } else {
      assert false : "Unserializable field type: "+valueType;
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
  protected void writeModel(final CharBuffer out, final Class<?> propertyType, final Model childModel, final PrimitiveSerializer primitives, final ModelSerializationContext ctx) {
    final ModelSerializer serializer = newSerializer(Class.class.cast(propertyType), ctx);
    final CharBuffer was = ctx.getBuffer();
    ctx.setBuffer(out);
    try {
      serializer.modelToString(childModel, ctx);
    } finally {
      ctx.setBuffer(was);
    }
  }

  protected <Mod extends Model> ModelSerializer<Mod> newSerializer(final Class<Mod> propertyType, final ModelSerializationContext ctx) {
    return new ModelSerializerDefault<Mod>(primitiveReaders);
  }

  @Override
  @SuppressWarnings("unchecked")
  public M modelFromString(final CharIterator src, final ModelDeserializationContext ctx) {
    final PrimitiveSerializer primitives = ctx.getPrimitives();
    final int modelState = primitives.deserializeInt(src);
    if (modelState == -2) {
      return null;
    }
    ModelKey key;
    if (modelState != -1) {
      // There is a key for this model
      final String keyString = src.consume(modelState).toString();
      key = ctx.getService().keyFromString(keyString);
    } else {
      key = null;
    }
    final M model = (M) ctx.getModel();
    assert model != null : "Null model found "+src;
    model.setKey(key);
    for (final String propertyName : model.getPropertyNames()) {
      if (preventDeserialization(model, propertyName, ctx)) {
        continue;
      }
      readProperty(model, propertyName, src, ctx);
    }
    return model;
  }

  protected void readProperty(final Model model, final String propertyName, final CharIterator src, final ModelDeserializationContext ctx) {
    final Class<?> propertyType = model.getPropertyType(propertyName);
    final PrimitiveSerializer primitives = ctx.getPrimitives();
    if (propertyType.isArray()) {
      model.setProperty(propertyName, readArray(propertyType.getComponentType(), src, primitives, ctx));
    } else if (propertyType.isPrimitive()) {
      final Object value = readPrimitive(propertyType, src, primitives);
      model.setProperty(propertyName, value);
    } else {
      final Object value = readObject(propertyType, src, primitives, ctx);
      model.setProperty(propertyName, value);
    }
  }

  /**
   * @param componentType
   * @param src
   * @param primitives
   * @param ctx
   * @return
   */
  protected Object readArray(final Class<?> componentType, final CharIterator src, final PrimitiveSerializer primitives,
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
        final Object value = readObject(componentType, src, primitives, ctx);
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
   * @param src
   * @param ctx
   * @param primitives
   * @return
   */
  @SuppressWarnings({
      "unchecked", "rawtypes"
  })
  protected Object readObject(final Class propertyType, final CharIterator src, final PrimitiveSerializer primitives, final ModelDeserializationContext ctx) {
    if (propertyType == String.class) {
      return readString(src, primitives);
    } else if (isModelType(propertyType)) {
      // We have an inner model to read!
      final ModelDeserializationContext context = ctx.createChildContext(propertyType);
      return modelFromString(src, context);
    } else if (isModelKeyType(propertyType)) {
      String key = readString(src, primitives);
      return X_Model.keyFromString(key);
    } else if (propertyType.isArray()) {
      return readArray(propertyType.getComponentType(), src, primitives, ctx);
    } else if (isIterableType(propertyType)) {
      return readIterable(propertyType, src, primitives, ctx);
    } else if (isStringMapType(propertyType)) {
      return readStringMap(propertyType, src, primitives, ctx);
    } else if (propertyType.isEnum()) {
      // No great way to deserialize enums without reflection, so lets leave a hook for environments
      // where reflection is not possible or preferable can implement a mapping of enum class to enum values[]...
      return readEnum(propertyType, src, primitives, ctx);
    }
    throw new UnsupportedOperationException("Unable to deserialize object of type "+propertyType);
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
