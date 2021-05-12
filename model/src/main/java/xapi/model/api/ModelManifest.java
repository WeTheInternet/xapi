/**
 *
 */
package xapi.model.api;

import xapi.annotation.model.ClientToServer;
import xapi.annotation.model.DeleterFor;
import xapi.annotation.model.FieldValidator;
import xapi.annotation.model.GetterFor;
import xapi.annotation.model.IsModel;
import xapi.annotation.model.PersistenceStrategy;
import xapi.annotation.model.Persistent;
import xapi.annotation.model.Serializable;
import xapi.annotation.model.SerializationStrategy;
import xapi.annotation.model.ServerToClient;
import xapi.annotation.model.SetterFor;
import xapi.dev.source.CharBuffer;
import xapi.log.X_Log;
import xapi.model.X_Model;
import xapi.model.impl.ModelNameUtil;
import xapi.model.impl.ModelUtil;
import xapi.source.lex.CharIterator;
import xapi.source.lex.StringCharIterator;
import xapi.validate.ValidatesValue;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This class is a manifest used to collect non-platform-specific metadata about all of the
 * methods of a given model class.  It will assemble and hold the field names of all the
 * user-specified fields actually added to the model, including any annotations that specify
 * behavior, such as being client-to-server/server-to-client only, whether the field should
 * be encrypted during transmission, etc.
 *
 *
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public class ModelManifest {

  public static class MethodData {

    private final String name;
    private SerializationStrategy c2sSerializer = SerializationStrategy.ProtoStream;
    private SerializationStrategy s2cSerializer = SerializationStrategy.ProtoStream;
    private boolean c2sEnabled = true;
    private boolean s2cEnabled = true;
    private boolean c2sEncrypted = false;
    private boolean s2cEncrypted = false;
    private boolean obfuscated;
    private PersistenceStrategy persistenceStrategy;
    private final ArrayList<Class<? extends ValidatesValue<?>>> validators;
    private Class<?> type;
    private Class<?>[] typeParams;
    private String idField;

    Map<String, ModelMethodType> methodNames = new HashMap<>();

    public MethodData(final String name) {
      this.name = name;
      this.validators = new ArrayList<>();
      idField = "id";
    }

    public MethodData(final String name, String idField, final GetterFor getter, final SetterFor setter, final DeleterFor deleter) {
      this.validators = new ArrayList<>();
      this.idField = idField;
      this.name = recordMethod(name, getter, setter, deleter);
    }
    /**
     * @return -> name
     */
    public String getName() {
      return name;
    }

    public void addAnnotations(final Annotation[] annos) {
      for (final Annotation anno : annos) {
        // Process all of the various types of annotations that we care about.
        if (anno instanceof Serializable) {
          final Serializable serializable = (Serializable) anno;

          obfuscated = serializable.obfuscated();

          final ClientToServer c2s = serializable.clientToServer();
          c2sSerializer = c2s.serializer();
          c2sEnabled = c2s.enabled();
          c2sEncrypted = c2s.encrypted();

          final ServerToClient s2c = serializable.serverToClient();
          s2cSerializer = s2c.serializer();
          s2cEnabled = s2c.enabled();
          s2cEncrypted = s2c.encrypted();

        } else if (anno instanceof Persistent) {
          final Persistent persistent = (Persistent) anno;
          persistenceStrategy = persistent.strategy();
        } else if (anno instanceof FieldValidator) {
          final FieldValidator validator = (FieldValidator) anno;
          for (final Class<? extends ValidatesValue<?>> validatesValue : validator.validators()) {
            validators.add(validatesValue);
          }
        } else {
          X_Log.trace(getClass(), "Unhandled annotation ",anno+" in ModelManifest.MethodData.addAnnotatons");
        }
      }
    }

    public void setIdField(String idField) {
      this.idField = idField;
    }

    public void setType(final Class<?> type) {
      this.type = type;
    }

    public Class<?> getType() {
      return type;
    }

    public void setTypeParams(final Class<?> ... typeParams) {
      this.typeParams = typeParams;
    }

    public Class<?>[] getTypeParams() {
      return typeParams;
    }

    public boolean isGetter(final String name) {
      return methodNames.get(name) == ModelMethodType.GET;
    }

    public String recordMethod(final String methodName, final GetterFor getter, final SetterFor setter, final DeleterFor deleter) {
      final ModelMethodType methodType = ModelMethodType.deduceMethodType(methodName, idField, getter, setter, deleter);
      final String name;
      switch (methodType) {
        case ID:
          name = idField;
          break;
        case GET:
          if (getter == null || getter.value().length() == 0) {
            name = ModelNameUtil.stripGetter(methodName);
          } else {
            name = getter.value();
          }
          break;
        case SET:
        case ADD:
        case ADD_ALL:
          if (setter == null || setter.value().length() == 0) {
            name = ModelNameUtil.stripSetter(methodName);
          } else {
            name = setter.value();
          }
          break;
        case REMOVE:
        case CLEAR:
          if (deleter == null || deleter.value().length() == 0) {
            name = ModelNameUtil.stripRemover(methodName);
          } else {
            name = deleter.value();
          }
          break;
        default:
          throw new UnsupportedOperationException("Method "+methodName+" is not a valid model data accessor");
      }
      methodNames.put(methodName, methodType);
      assert this.name == null || this.name.equals(name) : "Malformed property naming collision; "
          + "Model of type "+type+" has a field "+this.name + "which has received a new property "
          + "with the name "+name;
      return name;
    }

    public ModelMethodType getMethodType(final String methodName) {
      return methodNames.get(methodName);
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj instanceof MethodData) {
        final MethodData other = (MethodData) obj;
        if (!other.name.equals(name)) {
          return false;
        }
        if (other.type != type) {
          return false;
        }
        if (other.c2sEnabled != c2sEnabled) {
          return false;
        }
        if (other.s2cEnabled != s2cEnabled) {
          return false;
        }
        if (other.c2sEncrypted != c2sEncrypted) {
          return false;
        }
        if (other.s2cEncrypted != s2cEncrypted) {
          return false;
        }
        if (other.obfuscated != obfuscated) {
          return false;
        }
        if (other.persistenceStrategy != persistenceStrategy) {
          return false;
        }
        if (other.validators.size() != validators.size()) {
          return false;
        }
        for (int i = 0; i < validators.size(); i++) {
          if (other.validators.get(i) != validators.get(i)) {
            return false;
          }
        }
        if (other.methodNames.size() != methodNames.size()) {
          return false;
        }
        for (final Entry<String, ModelMethodType> entry : other.methodNames.entrySet()) {
          final ModelMethodType type = methodNames.get(entry.getKey());
          if (type != entry.getValue()) {
            return false;
          }
        }
        return true;
      }
      return false;
    }

    @Override
    public int hashCode() {
      return name.hashCode() ^ type.hashCode();
    }

    static CharBuffer serializeData(final CharBuffer out, final MethodData data, final PrimitiveSerializer primitives) {
      out.append(primitives.serializeString(data.name));
      out.append(primitives.serializeClass(data.type));
      out.append(primitives.serializeBooleanArray(
        data.c2sEnabled,
        data.s2cEnabled,
        data.c2sEncrypted,
        data.s2cEncrypted,
        data.obfuscated
      ));
      out.append(primitives.serializeInt(data.c2sSerializer.ordinal()));
      out.append(primitives.serializeInt(data.s2cSerializer.ordinal()));
      if (data.persistenceStrategy == null) {
        out.append(primitives.serializeInt(-1));
      } else {
        out.append(primitives.serializeInt(data.persistenceStrategy.ordinal()));
      }
      out.append(primitives.serializeInt(data.validators.size()));
      for (final Class<? extends ValidatesValue<?>> validator : data.validators) {
        out.append(primitives.serializeClass(validator));
      }
      out.append(primitives.serializeInt(data.methodNames.size()));
      for (final Entry<String, ModelMethodType> entry : data.methodNames.entrySet()) {
        out.append(primitives.serializeInt(entry.getValue().ordinal()));
        if (entry.getValue().isDefaultName(entry.getKey(), data.name)) {
          // When the name matches the default, we will serialize null to save space;
          // we will recalculate the default name when deserializing
          out.append(primitives.serializeString(null));
        } else {
          out.append(primitives.serializeString(entry.getKey()));
        }
      }
      return out;
    }

    static MethodData deserializeData(final CharIterator chars, final PrimitiveSerializer primitives) {
      final String name = primitives.deserializeString(chars);
      final MethodData data = new MethodData(name);
      data.type = primitives.deserializeClass(chars);

      final boolean[] bools = primitives.deserializeBooleanArray(chars);

      data.c2sEnabled = bools[0];
      data.s2cEnabled = bools[1];
      data.c2sEncrypted = bools[2];
      data.s2cEncrypted = bools[3];
      data.obfuscated = bools[4];

      int ordinal = primitives.deserializeInt(chars);
      data.c2sSerializer = SerializationStrategy.values()[ordinal];

      ordinal = primitives.deserializeInt(chars);
      data.s2cSerializer = SerializationStrategy.values()[ordinal];

      ordinal = primitives.deserializeInt(chars);
      if (ordinal != -1) {
        data.persistenceStrategy = PersistenceStrategy.values()[ordinal];
      }

      int numValidators = primitives.deserializeInt(chars);
      while(numValidators --> 0) {
        data.validators.add(primitives.<ValidatesValue<?>>deserializeClass(chars));
      }
      int numMethods = primitives.deserializeInt(chars);
      while (numMethods --> 0) {
        ordinal = primitives.deserializeInt(chars);
        String methodName = primitives.deserializeString(chars);
        final ModelMethodType methodType = ModelMethodType.values()[ordinal];
        if (methodName == null) {
          methodName = methodType.getDefaultName(name);
        }
        data.methodNames.put(methodName, methodType);
      }
      return data;
    }

  }

  private final Map<String, MethodData> methodsByPropertyNames;
  private final Map<String, MethodData> methodsByMethodNames;
  private final Annotation[] defaultAnnotations;
  private final String type;
  private String[] properties;
  private final Class<? extends Model> modelType;

  public static String serialize(final ModelManifest manifest) {
    return serialize(new CharBuffer(), manifest, X_Model.getService().primitiveSerializer()).toSource();
  }

  public static CharBuffer serialize(CharBuffer out, final ModelManifest manifest, final PrimitiveSerializer primitives) {

    if (out == null) {
      out = new CharBuffer();
    }
    out.append(primitives.serializeString(manifest.type));
    out.append(primitives.serializeClass(manifest.modelType));

    out.append(primitives.serializeInt(manifest.getPropertyNames().length));
    for (final String property : manifest.properties) {
      out.append(primitives.serializeString(property));
    }

    for (final String property : manifest.properties) {
      final MethodData data = manifest.methodsByPropertyNames.get(property);
      MethodData.serializeData(out, data, primitives);
    }

    return out;
  }

  public static ModelManifest deserialize(final String asString) {
    return deserialize(new StringCharIterator(asString), X_Model.getService().primitiveSerializer());
  }

  @SuppressWarnings({
      "rawtypes", "unchecked"
  })
  public static ModelManifest deserialize(final CharIterator chars, final PrimitiveSerializer primitives) {

    final String typeName = primitives.deserializeString(chars);
    final Class modelType = primitives.deserializeClass(chars);
    final ModelManifest manifest = new ModelManifest(typeName, modelType);

    final int numProps = primitives.deserializeInt(chars);
    manifest.properties = new String[numProps];

    for (int i = 0;i < numProps; i++) {
      manifest.properties[i] = primitives.deserializeString(chars);
    }

    for (final String property : manifest.properties) {
      final MethodData data = MethodData.deserializeData(chars, primitives);
      manifest.methodsByPropertyNames.put(property, data);
    }

    for (final String property : manifest.properties) {
      final MethodData data = manifest.methodsByPropertyNames.get(property);
      for (final String methodName : data.methodNames.keySet()) {
        manifest.methodsByMethodNames.put(methodName, data);
      }
    }

    return manifest;
  }

  private ModelManifest(final String typeName, final Class<? extends Model> cls) {
    methodsByPropertyNames = new LinkedHashMap<>();
    methodsByMethodNames = new HashMap<>();
    type = typeName;
    this.modelType = cls;
    // Unused for this constructor
    defaultAnnotations = new Annotation[0];
  }

  public ModelManifest(final Class<? extends Model> cls) {
    methodsByPropertyNames = new LinkedHashMap<>();
    methodsByMethodNames = new HashMap<>();
    this.modelType = cls;
    final IsModel isModel = cls.getAnnotation(IsModel.class);
    if (isModel == null) {
      // Guess the type name from the model's classname.
      String name = ModelUtil.guessModelType(cls.getSimpleName());
      if (name.length() == 0) {
        name = "model";
      }
      type = Character.toLowerCase(name.charAt(0)) + name.substring(1);
      defaultAnnotations = new Annotation[0];
    } else {
      type = isModel.modelType();
      defaultAnnotations = new Annotation[] {
          isModel.persistence(), isModel.serializable()
      };
      if (isModel.propertyOrder().length > 0) {
        properties = isModel.propertyOrder();
      }
    }
  }

  public MethodData addProperty(final String methodName, String idField, final GetterFor getter, final SetterFor setter, final DeleterFor deleter) {
    MethodData data;
    if (methodsByMethodNames.containsKey(methodName)) {
      data = methodsByMethodNames.get(methodName);
      data.recordMethod(methodName, getter, setter, deleter);
    } else {
      data = new MethodData(methodName, idField, getter, setter, deleter);
      if (methodsByPropertyNames.containsKey(data.name)) {
        data = methodsByPropertyNames.get(data.name);
        data.recordMethod(methodName, getter, setter, deleter);
      } else {
        if (defaultAnnotations.length > 0) {
          data.addAnnotations(defaultAnnotations);
        }
      }
      methodsByMethodNames.put(methodName, data);
    }
    methodsByPropertyNames.put(data.getName(), data);

    return data;
  }

  public SerializationStrategy getClientToServerSerializationStrategy(final String name) {
    return getMethodData(name).c2sSerializer;
  }

  public SerializationStrategy getServerToClientSerializationStrategy(final String name) {
    return getMethodData(name).s2cSerializer;
  }

  public boolean isClientToServerEnabled(final String name) {
    return getMethodData(name).c2sEnabled;
  }

  public boolean isClientToServerEncrypted(final String name) {
    return getMethodData(name).c2sEncrypted;
  }

  public boolean isServerToClientEnabled(final String name) {
    return getMethodData(name).s2cEnabled;
  }

  public boolean isServerToClientEncrypted(final String name) {
    return getMethodData(name).s2cEncrypted;
  }

  public boolean isSerializationObfuscated(final String name) {
    return getMethodData(name).obfuscated;
  }

  public PersistenceStrategy getPersistenceStrategy(final String name) {
    return getMethodData(name).persistenceStrategy;
  }

  @SuppressWarnings("unchecked")
  public Class<? extends ValidatesValue<?>>[] getValidatorTypes(final String name) {
    return getMethodData(name).validators.toArray(new Class[0]);
  }

  public MethodData getMethodData(final String name) {
    MethodData data = methodsByPropertyNames.get(name);
    if (data == null) {
      data = methodsByMethodNames.get(name);
      if (data == null) {
        throw new UnsupportedOperationException("Cannot find MethodData for property "+name);
      }
    }
    return data;
  }

  public ModelMethodType getMethodType(final String name) {
    final MethodData data = getMethodData(name);
    return data.getMethodType(name);
  }

  public boolean hasSeenMethod(final String methodName) {
    return methodsByMethodNames.containsKey(methodName);
  }

  public String getType() {
    return type;
  }

  public Class<? extends Model> getModelType() {
    return modelType;
  }

  public String[] getPropertyNames() {
    if (properties == null) {
      properties = methodsByPropertyNames.keySet().toArray(new String[methodsByPropertyNames.size()]);
    }
    return properties;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof ModelManifest) {
      final ModelManifest other = (ModelManifest) obj;
      if (!other.type.equals(type)) {
        return false;
      }
      if (!Arrays.equals(other.getPropertyNames(), getPropertyNames())) {
        return false;
      }
      if (other.methodsByPropertyNames.size() != methodsByPropertyNames.size()) {
        return false;
      }
      if (other.methodsByMethodNames.size() != methodsByMethodNames.size()) {
        return false;
      }
      if (!other.methodsByMethodNames.keySet().containsAll(methodsByMethodNames.keySet())) {
        return false;
      }
      for (final Entry<String, MethodData> entry : methodsByPropertyNames.entrySet()) {
        final MethodData data = other.methodsByPropertyNames.get(entry.getKey());
        if (data == null) {
          return false;
        }
        if (!data.equals(entry.getValue())) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = type.hashCode();
    hash ^= properties.length;
    for (final String property : properties) {
      hash ^= property.hashCode();
    }
    // We don't bother hashing any deeper; any object with the same type and properties should ==
    return hash;
  }

  /**
   * For now, we are setting all properties to indexed; we will map an annotation for this
   * property in the future
   */
  public boolean isIndexed(final String propertyName) {
    return true;
  }
}
