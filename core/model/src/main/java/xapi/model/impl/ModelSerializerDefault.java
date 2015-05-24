/**
 *
 */
package xapi.model.impl;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import xapi.dev.source.CharBuffer;
import xapi.log.X_Log;
import xapi.model.api.Model;
import xapi.model.api.ModelDeserializationContext;
import xapi.model.api.ModelKey;
import xapi.model.api.ModelManifest;
import xapi.model.api.ModelSerializationContext;
import xapi.model.api.ModelSerializer;
import xapi.model.api.PrimitiveReader;
import xapi.model.api.PrimitiveSerializer;
import xapi.source.api.CharIterator;
import xapi.util.X_Debug;

public class ModelSerializerDefault <M extends Model> implements ModelSerializer<M>{

  private final Map<Class<?>, PrimitiveReader> primitiveReaders;

  public ModelSerializerDefault() {
    this(new HashMap<>());
  }

  public ModelSerializerDefault(final Map<Class<?>, PrimitiveReader> primitiveReaders) {
    this.primitiveReaders = primitiveReaders;
  }

  @Override
  public CharBuffer modelToString(final M model, final ModelSerializationContext ctx) {
    final CharBuffer out = new CharBuffer();
    ctx.getBuffer().addToEnd(out);
    write(model, out, ctx);
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
      if (propertyType.isArray()) {
        // write an array
        writeArray(out, propertyType, value, primitives, ctx);
      } else if (propertyType == String.class) {
        final String asString = (String) value;
        writeString(out, asString, primitives);
      } else if (propertyType.isPrimitive()) {
        // write a primitive
        if (propertyType == double.class) {
          Double asDouble = (Double) value;
          if (asDouble == null) {
            asDouble = 0.;
          }
          out.append(primitives.serializeDouble(asDouble.doubleValue()));
        } else if (propertyType == float.class) {
          Float asFloat = (Float) value;
          if (asFloat == null) {
            asFloat = 0f;
          }
          out.append(primitives.serializeFloat(asFloat.floatValue()));
        } else if (propertyType == long.class) {
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
      } else if (isModelType(propertyType)) {
        writeModel(out, propertyType, (Model)value, primitives, ctx);
      } else if (isSupportedEnumType(propertyType)) {
        if (value == null) {
          out.append(primitives.serializeInt(-1));
        } else {
          final Enum asEnum = (Enum) value;
          out.append(primitives.serializeInt(asEnum.ordinal()));
        }
      } else {
        assert false : "Unserializable field type: "+propertyType;
      }
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
    if (modelState > -1) {
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

  protected PrimitiveReader getPrimitiveReader(final Class<?> componentType, final Map<Class<?>, PrimitiveReader> primitiveReaders) {
    PrimitiveReader reader = primitiveReaders.get(componentType);
    if (reader == null) {
      if (componentType == int.class) {
        reader = (cls, src, primitives) -> primitives.deserializeInt(src);
      } else if (componentType == float.class) {
        reader = (cls, src, primitives) -> primitives.deserializeFloat(src);
      } else if (componentType == boolean.class) {
        reader = (cls, src, primitives) -> primitives.deserializeBoolean(src);
      } else if (componentType == char.class) {
        reader = (cls, src, primitives) -> primitives.deserializeChar(src);
      } else if (componentType == double.class) {
        reader = (cls, src, primitives) -> primitives.deserializeDouble(src);
      } else if (componentType == long.class) {
        reader = (cls, src, primitives) -> primitives.deserializeLong(src);
      } else if (componentType == short.class) {
        reader = (cls, src, primitives) -> primitives.deserializeShort(src);
      } else if (componentType == byte.class) {
        reader = (cls, src, primitives) -> primitives.deserializeByte(src);
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
      final int len = primitives.deserializeInt(src);
      if (len == -1) {
        return null;
      }
      return src.consume(len);
    } else if (isModelType(propertyType)) {
      // We have an inner model to read!
      final ModelDeserializationContext context = ctx.createChildContext(propertyType, src);
      return modelFromString(src, context);
    } else if (propertyType.isArray()) {
      return readArray(propertyType.getComponentType(), src, primitives, ctx);
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
