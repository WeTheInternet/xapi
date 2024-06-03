/**
 *
 */
package xapi.model.impl;

import xapi.annotation.compile.MagicMethod;
import xapi.annotation.model.ClientToServer;
import xapi.annotation.model.KeyOnly;
import xapi.collect.X_Collect;
import xapi.collect.api.ClassTo;
import xapi.collect.api.StringTo;
import xapi.dev.source.CharBuffer;
import xapi.fu.In1;
import xapi.fu.In2;
import xapi.inject.X_Inject;
import xapi.model.api.*;
import xapi.model.service.ModelService;
import xapi.model.tools.ModelSerializerDefault;
import xapi.model.tools.PrimitiveSerializerDefault;
import xapi.source.lex.CharIterator;
import xapi.source.lex.StringCharIterator;
import xapi.string.X_String;
import xapi.time.X_Time;
import xapi.util.api.SuccessHandler;

import java.util.Objects;

@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class AbstractModelService implements ModelService
{

  protected final StringTo<ModelSerializer> serializers = X_Collect.newStringMap(ModelSerializer.class);
  protected final ClassTo<String> classToTypeName = X_Collect.newClassMap(String.class);
  protected final StringTo<Class<? extends Model>> typeNameToClass = X_Collect.newStringMap(Class.class.cast(Class.class));

  @Override
  @MagicMethod(doNotVisit=true, documentation="This magic method re-routes to the same provider as X_Inject.instance()")
  public final <T extends Model> T create(final Class<T> key) {
    return doCreate(key);
  }

  protected abstract boolean isAsync();

  @MagicMethod(doNotVisit=true, documentation="This magic method re-routes to the same provider as X_Inject.instance()")
  protected <T extends Model> T doCreate(final Class<T> key) {
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
    final ModelDeserializationContext context = new ModelDeserializationContext(doCreate(cls), this, findManifest(cls));
    context.setClientToServer(isClientToServer());
    boolean isKeyOnly = cls.getAnnotation(KeyOnly.class) != null || context.isKeyOnly();
    final ModelSerializer<M> serializer = getSerializer(getTypeName(cls));
    if (isKeyOnly) {
      context.setKeyOnly(isKeyOnly);
    }
    return serializer.modelFromString(cls, model, context, isKeyOnly);
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
    context.setKeyOnly(manifest.isKeyOnly());
    final ModelSerializer<M> serializer = getSerializer(manifest.getType());
    return serializer.modelFromString(cls, model, context, manifest.isKeyOnly());
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

  protected <M extends Model> void serialize(final String type, final M model, In2<CharBuffer, Throwable> callback) {
      if (isAsync()) {
          X_Time.runLater(()->{
            doSerialize(type, model, callback);
          });
      } else {
        doSerialize(type, model, callback);
      }
  }

    private <M extends Model> void doSerialize(final String type, final M model, final In2<CharBuffer, Throwable> callback) {
        final CharBuffer buffer;
        try {
            buffer = serialize(type, model);
        } catch (Throwable t) {
            callback.in(null, t);
            return;
        }
        callback.in(buffer, null);
    }

  protected <M extends Model> CharBuffer serialize(final String type, final M model) {
    if (model == null) {
      return null;
    }

    final CharBuffer buffer = new CharBuffer();

    final ModelSerializationContext context = new ModelSerializationContext(buffer, this, findManifest(type));
    context.setClientToServer(isClientToServer());
    return getSerializer(type).modelToString(getModelType(model), model, context, false);
  }

  protected ModelManifest findManifest(final Class<?> type) {
    String stringType = classToTypeName.get(type);
    return stringType == null ? null : findManifest(stringType);
  }
  protected ModelManifest findManifest(final String type) {

    return null;
  }

  protected <M extends Model> Class<? extends Model> getModelType(final M model) {
    final Class<?>[] ifaces = model.getClass().getInterfaces();
    if (ifaces != null) {
      for (Class iface : ifaces) {
        if (Model.class.isAssignableFrom(iface) && iface != Model.class) {
          return iface;
        }
      }
    }
    return model.getClass();
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
    return serializer.modelToString(manifest.getModelType(), model, context, false);
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
    if (parentState < 0) {
      if (parentState == -1) {
        // null key
        return null;
      }

      // no key, or no parent key specified
      final String namespace;
      if (parentState == -2) {
        namespace = "";
      } else {
        assert parentState == -3 : "Malformed key started with parentState " + parentState + " \nremaining: " + chars;
        namespace = primitives.deserializeString(chars);
      }
      final String kind = primitives.deserializeString(chars);
      final int keyType = primitives.deserializeInt(chars);
      final String id = primitives.deserializeString(chars);
      return newKey(namespace, kind, id).setKeyType(keyType);
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
      String ns = key.getNamespace();
      if (X_String.isEmpty(ns)) {
        b.append(primitives.serializeInt(-2));
      } else {
        b.append(primitives.serializeInt(-3));
        b.append(primitives.serializeString(key.getNamespace()));
      }
    } else {
      // we double-serialize this string, such that its serialized form
      // (with a length followed by chars) either starts with 0 or a positive integer.
      // this is what allows us to use negative numbers for "special address space".
      final String parentKey = keyToString(key.getParent());
      b.append(primitives.serializeString(parentKey));
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

  @Override
  public void flushCaches() {
    this.classToTypeName.clear();
    this.typeNameToClass.clear();
    this.serializers.clear();
  }

  @Override
  public <M extends Model> void query(final ModelManifest manifest, final ModelQuery<M> query, final SuccessHandler<ModelQueryResult<M>> callback) {
    query((Class)manifest.getModelType(), query, callback);
  }
}
