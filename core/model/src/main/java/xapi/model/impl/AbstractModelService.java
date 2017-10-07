/**
 *
 */
package xapi.model.impl;

import java.util.Objects;

import xapi.annotation.compile.MagicMethod;
import xapi.collect.X_Collect;
import xapi.collect.api.ClassTo;
import xapi.collect.api.StringTo;
import xapi.dev.source.CharBuffer;
import xapi.inject.X_Inject;
import xapi.model.api.Model;
import xapi.model.api.ModelDeserializationContext;
import xapi.model.api.ModelKey;
import xapi.model.api.ModelManifest;
import xapi.model.api.ModelSerializationContext;
import xapi.model.api.ModelSerializer;
import xapi.model.api.PrimitiveSerializer;
import xapi.model.service.ModelService;
import xapi.source.api.CharIterator;
import xapi.source.impl.StringCharIterator;
import xapi.util.api.SuccessHandler;

@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class AbstractModelService implements ModelService
{

  protected final StringTo<ModelSerializer> serializers = X_Collect.newStringMap(ModelSerializer.class);
  protected final ClassTo<String> classToTypeName = X_Collect.newClassMap(String.class);
  protected final StringTo<Class<? extends Model>> typeNameToClass = X_Collect.newStringMap(Class.class.cast(Class.class));

  @Override
  @MagicMethod(doNotVisit=true, documentation="This magic method re-routes to the same provider as X_Inject.instance()")
  public <T extends Model> T create(final Class<T> key) {
    // GWT dev will make it here, and it can handle non-class-literal injection.
    // GWT prod requires magic method injection here.
    final T instance = X_Inject.instance(key);
    if (instance == null) {
      return null;
    }
    typeNameToClass.put(instance.getType(), key);
    classToTypeName.put(key, instance.getType());
    return instance;
  }

  @Override
  public <M extends Model> void persist(final M model, final SuccessHandler<M> callback) {
    assert Objects.nonNull(model) : "Cannot persist a null model";
    doPersist(model.getType(), model, callback);
  }

  protected abstract <M extends Model> void doPersist(String type, M model, SuccessHandler<M> callback);

  protected <M extends Model> M deserialize(final String cls, final CharIterator model) {
    return deserialize((Class<M>)typeNameToClass.get(cls), model);
  }

  @Override
  public <M extends Model> M deserialize(final Class<M> cls, final CharIterator model) {
    if (model == null) {
      return null;
    }
    final ModelDeserializationContext context = new ModelDeserializationContext(create(cls), this, null);
    context.setClientToServer(isClientToServer());
    final ModelSerializer<M> serializer = getSerializer(getTypeName(cls));
    return serializer.modelFromString(model, context);
  }

  /**
   * By default, all JRE environments will be considered server to client, and client
   * implementations of the model service will override this method to return true
   */
  protected boolean isClientToServer() {
    return false;
  }

  @Override
  public <M extends Model> M deserialize(final ModelManifest manifest, final CharIterator model) {
    if (model == null) {
      return null;
    }
    final Class<M> cls = (Class<M>) typeNameToClass.get(manifest.getType());
    final ModelDeserializationContext context = new ModelDeserializationContext(create(cls), this, manifest);
    context.setClientToServer(isClientToServer());
    final ModelSerializer<M> serializer = getSerializer(manifest.getType());
    return serializer.modelFromString(model, context);
  }

  @Override
  public PrimitiveSerializer primitiveSerializer() {
    return new PrimitiveSerializerDefault();
  }

  protected String getTypeName(final Class<? extends Model> cls) {
    String known = classToTypeName.get(cls);
    if (known != null) {
      return known;
    }
    if (!cls.isInterface()) {
      // For non-interfaces, lets look for the most specfic Model interface
      Class<?> winner = cls;
      for (final Class<?> iface : cls.getInterfaces()) {
        if (Model.class.isAssignableFrom(iface)) {
          if (winner == cls || winner.isAssignableFrom(iface)) {
            winner = iface;
          }
        }
      }
      if (winner != cls) {
        known = classToTypeName.get(winner);
        if (known != null) {
          classToTypeName.put(cls, known);
          typeNameToClass.put(known, cls);
          return known;
        }
      }
    }
    final String name = ModelUtil.guessModelType(cls);
    classToTypeName.put(cls, name);
    typeNameToClass.put(name, cls);
    return name;
  }

  protected <M extends Model> ModelSerializer<M> getSerializer(final String type) {
    ModelSerializer serializer = serializers.get(type);
    if (serializer == null) {
      serializer = getDefaultSerializer(type);
    }
    return serializer;
  }

  protected <M extends Model> ModelSerializer getDefaultSerializer(final String type) {
    return new ModelSerializerDefault<M>();
  }

  @Override
  public <M extends Model> CharBuffer serialize(final Class<M> cls, final M model) {
    return serialize(getTypeName(cls), model);
  }

  protected <M extends Model> CharBuffer serialize(final String type, final M model) {
    if (model == null) {
      return null;
    }

    final CharBuffer buffer = new CharBuffer();
    final ModelSerializationContext context = new ModelSerializationContext(buffer, this, null);
    context.setClientToServer(isClientToServer());
    return getSerializer(type).modelToString(model, context);
  }

  @Override
  public <M extends Model> CharBuffer serialize(final ModelManifest manifest, final M model) {
    if (model == null) {
      return null;
    }

    final CharBuffer buffer = new CharBuffer();
    final ModelSerializationContext context = new ModelSerializationContext(buffer, this, manifest);
    context.setClientToServer(isClientToServer());
    final ModelSerializer<Model> serializer = getSerializer(manifest.getType());
    return serializer.modelToString(model, context);
  }

  @Override
  public String register(final Class<? extends Model> model) {
    final String typeName = getTypeName(model);
    classToTypeName.put(model, typeName);
    typeNameToClass.put(typeName, model);
    final ModelSerializer serializer = getSerializer(typeName);
    serializers.put(typeName, serializer);
    return typeName;
  }

  /**
   * @see xapi.model.service.ModelService#keyFromString(java.lang.String)
   */
  @Override
  public ModelKey keyFromString(final String key) {
    if (key == null) {
      return null;
    }
    final CharIterator chars = new StringCharIterator(key);
    final PrimitiveSerializer primitives = primitiveSerializer();
    return deserializeKey(chars, primitives);
  }

  protected ModelKey deserializeKey(final CharIterator chars, final PrimitiveSerializer primitives) {
    ModelKey parent = null;
    if (!chars.hasNext()) {
      return null;
    }
    final int parentState = primitives.deserializeInt(chars);
    if (parentState == -3) {// no parent key specified
      final String namespace = primitives.deserializeString(chars);
      final String kind = primitives.deserializeString(chars);
      final int keyType = primitives.deserializeInt(chars);
      final String id = primitives.deserializeString(chars);
      return newKey(namespace, kind, id).setKeyType(keyType);
    } else if (parentState == -1){
      // a null key...
      return null;
    } else {
      final String parentString = chars.consume(parentState).toString();
      assert parentString != null;
      parent = keyFromString(parentString);
      final String kind = primitives.deserializeString(chars);
      final int keyType = primitives.deserializeInt(chars);
      final String id = primitives.deserializeString(chars);
      return parent.getChild(kind, id).setKeyType(keyType);
    }
  }

  /**
   * @see xapi.model.service.ModelService#keyToString(xapi.model.api.ModelKey)
   */
  @Override
  public String keyToString(final ModelKey key) {
    final StringBuilder b = new StringBuilder();
    final PrimitiveSerializer primitives = primitiveSerializer();
    if (key == null) {
      return primitives.serializeInt(-1);
    }
    if (key.getParent() == null) {
      b.append(primitives.serializeInt(-3));
      b.append(primitives.serializeString(key.getNamespace()));
    } else {
      b.append(primitives.serializeString(keyToString(key.getParent())));
    }
    b.append(primitives.serializeString(key.getKind()));
    b.append(primitives.serializeInt(key.getKeyType()));
    b.append(primitives.serializeString(key.getId()));
    return b.toString();

  }

  /**
   * @see xapi.model.service.ModelService#newKey(java.lang.String, java.lang.String)
   */
  @Override
  public ModelKey newKey(final String namespace, final String kind) {
    return new ModelKeyDefault(namespace, kind);
  }

  /**
   * @see xapi.model.service.ModelService#newKey(java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public ModelKey newKey(final String namespace, final String kind, final String id) {
    return new ModelKeyDefault(namespace, kind, id);
  }

}
