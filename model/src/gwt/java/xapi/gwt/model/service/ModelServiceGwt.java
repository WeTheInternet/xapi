package xapi.gwt.model.service;

import xapi.annotation.compile.MagicMethod;
import xapi.annotation.inject.SingletonOverride;
import xapi.collect.X_Collect;
import xapi.collect.api.ClassTo;
import xapi.collect.api.StringDictionary;
import xapi.dev.source.CharBuffer;
import xapi.except.NotConfiguredCorrectly;
import xapi.fu.Out1;
import xapi.fu.itr.MappedIterable;
import xapi.io.X_IO;
import xapi.io.api.DelegatingIOCallback;
import xapi.log.X_Log;
import xapi.model.X_Model;
import xapi.model.api.*;
import xapi.model.impl.AbstractModelService;
import xapi.model.tools.ModelSerializerDefault;
import xapi.model.service.ModelService;
import xapi.platform.GwtPlatform;
import xapi.source.lex.StringCharIterator;
import xapi.util.api.ErrorHandler;
import xapi.util.api.ProvidesValue;
import xapi.util.api.SuccessHandler;

import java.lang.reflect.Method;

import com.google.gwt.core.client.GWT;

@GwtPlatform
@SuppressWarnings({"rawtypes"})
@SingletonOverride(implFor=ModelService.class)
public class ModelServiceGwt extends AbstractModelService
{

  @SuppressWarnings("unchecked")
  private static ClassTo<ProvidesValue<? extends Model>> PROVIDERS = X_Collect.newClassMap(
      Class.class.cast(ProvidesValue.class)
  );

  public static String REGISTER_CREATOR_METHOD = "registerCreator";

  private static Class<? extends Model> implClassRef;
  public static <M extends Model> String registerCreator(final Class<M> cls, final String type, final Class<? extends M> implClass, final ProvidesValue<M> provider) {
    PROVIDERS.put(cls, provider);
    implClassRef = implClass;
    final ModelService modelService = X_Model.getService();
    if (modelService instanceof ModelServiceGwt) {
      final ModelServiceGwt service = (ModelServiceGwt) modelService;
      service.classToTypeName.put(cls, type);
      service.classToTypeName.put(implClass, type);
      service.typeNameToClass.put(type, cls);
    } else {
      return doRegister(modelService, cls);
    }
    implClassRef = null;
    return type;
  }

  @MagicMethod(doNotVisit = false)
  private static <M extends Model> String doRegister(ModelService modelService, Class<M> cls) {
    return modelService.register(cls);
  }

  @Override
  protected boolean isAsync() {
    return false;
  }

  protected <T extends Model> T doCreate(final Class<T> key) {
    final ProvidesValue<? extends Model> provider = PROVIDERS.get(key);
    if (provider == null) {
      throw new NotConfiguredCorrectly("Could not find provider for "+key+"; did you forget to call X_Model.register()?");
    }
    return resolveProvider(key, provider);
  }

  protected <T extends Model> T resolveProvider(Class<T> key, ProvidesValue<? extends Model> provider) {
    if (!classToTypeName.containsKey(key)) {
      final T model = (T) provider.get();
      classToTypeName.put(key, model.getType());
      return model;
    }
    return (T) provider.get();
  }

  /**
   * @see xapi.model.impl.AbstractModelService#getDefaultSerializer(java.lang.String)
   */
  @Override
  protected <M extends Model> ModelSerializer getDefaultSerializer(final String typeName) {

    return new ModelSerializerDefault<Model>() {
      @Override
      protected boolean isModelType(final Class<?> propertyType) {
        return PROVIDERS.containsKey(propertyType);
      }

      @Override
      protected boolean isIterableType(Class<?> propertyType) {
        return super.isIterableType(propertyType);
      }
    };
  }

  @Override
  public String register(final Class<? extends Model> model) {
    super.register(model);
    final String type = classToTypeName.get(model);
    if (implClassRef != null) {
      if (type != null) {
        classToTypeName.put(implClassRef, type);
      }
    }
    return type;
  }

  @Override
  protected <M extends Model> void doPersist(final String type, final M model, final SuccessHandler<M> callback) {
    final String url = getUrlBase()+"model/persist";
    final StringDictionary<String> headers = X_Collect.newDictionary();
    headers.setValue("X-Model-Type", model.getType());
    X_Log.warn(ModelServiceGwt.class, this, model);
    final CharBuffer serialized = serialize(type, model);
    X_IO.getIOService().post(url, serialized.toString(), headers, new DelegatingIOCallback<>(
            resp -> {
      final M deserialized = deserialize(type, new StringCharIterator(resp.body()));
      callback.onSuccess(deserialized);
    }, DelegatingIOCallback.failHandler(callback)));
  }

  @Override
  public void delete(final ModelKey key, final SuccessHandler<Boolean> callback) {
    final String url = getUrlBase()+"model/persist";
    final StringDictionary<String> headers = X_Collect.newDictionary();
    headers.setValue("X-Model-Type", key.getKind());
    X_Log.warn(ModelServiceGwt.class, this, "deleting model", key);
    X_IO.getIOService().delete(url, headers, new DelegatingIOCallback<>(
            resp -> {
          callback.onSuccess(resp.statusCode() == 200);
    }, DelegatingIOCallback.failHandler(callback)));

  }

  protected String getUrlBase() {
    return GWT.getModuleBaseURL().replace("/" + GWT.getModuleName(), "");
  }

  @Override
  public <M extends Model> void load(final Class<M> type, final ModelKey modelKey, final SuccessHandler<M> callback) {
    final String url = getUrlBase()+"model/load";
    final String typeName = getTypeName(type);
    final StringDictionary<String> headers = X_Collect.newDictionary();
    headers.setValue("X-Model-Type", typeName);
    final PrimitiveSerializer primitives = primitiveSerializer();
    String ns = modelKey.getNamespace().length() == 0 ? "" : modelKey.getNamespace();
    // The namespace might be empty, so we use the serialized form that can transmit "" without
    // constructing an invalid uri.
    ns = primitives.serializeString(ns);
    final String kind = modelKey.getKind();
    // THIS DOESN'T TAKE PARENT KEYS INTO ACCOUNT!
    final String id = primitives.serializeInt(modelKey.getKeyType()) + modelKey.getId();
    final String serialized = "/" + ns + "/"+kind+"/"+id;
    X_IO.getIOService().get(url+serialized, headers, new DelegatingIOCallback<>(resp -> {
      X_Log.error("Got response! "+resp.body());
      final M deserialized = deserialize(type, new StringCharIterator(resp.body()));
      callback.onSuccess(deserialized);
    }, f-> {
      X_Log.error(ModelServiceGwt.class, "Load of ", type, modelKey, "failed", f);
      if (callback instanceof ErrorHandler) {
        ((ErrorHandler) callback).onError(f);
      }
    }));

  }

  @Override
  public <M extends Model> void query(final Class<M> modelClass, final ModelQuery<M> query,
      final SuccessHandler<ModelQueryResult<M>> callback) {
    final String url = getUrlBase()+"model/query";
    final String typeName = getTypeName(modelClass);
    final StringDictionary<String> headers = X_Collect.newDictionary();
    headers.setValue("X-Model-Type", typeName);
    final PrimitiveSerializer primitives = primitiveSerializer();
    String ns = query.getNamespace().length() == 0 ? "" : query.getNamespace();
    // The namespace might be empty, so we use the serialized form that can transmit "" without
    // constructing an invalid uri.
    ns = primitives.serializeString(ns);
    final String kind = primitives.serializeString(typeName);
    final String serialized = "/" + ns + "/"+kind+"/"+query.serialize(this, primitives);
    X_IO.getIOService().get(url+serialized, headers, new DelegatingIOCallback<>(resp -> {
      X_Log.error("Got response! "+resp.body());
      final StringCharIterator chars = new StringCharIterator(resp.body());
      final String cursor = primitives.deserializeString(chars);
      int numResults = primitives.deserializeInt(chars);
      final ModelQueryResult<M> result = new ModelQueryResult<>(modelClass);
      result.setCursor(cursor);
      while (numResults --> 0) {
        final M deserialized = deserialize(typeName, chars);
        result.addModel(deserialized);
      }
      callback.onSuccess(result);
    }, DelegatingIOCallback.failHandler(callback)));

  }

  @Override
  public void query(final ModelQuery<Model> query, final SuccessHandler<ModelQueryResult<Model>> callback) {
    final String url = getUrlBase()+"model/query";
    final StringDictionary<String> headers = X_Collect.newDictionary();
    final PrimitiveSerializer primitives = primitiveSerializer();
    String ns = query.getNamespace().length() == 0 ? "" : query.getNamespace();
    // The namespace might be empty, so we use the serialized form that can transmit "" without
    // constructing an invalid uri.
    ns = primitives.serializeString(ns);
    final String kind = primitives.serializeString("");
    final String serialized = "/" + ns + "/" + kind + "/" + query.serialize(this, primitives);
    X_IO.getIOService().get(url+serialized, headers, new DelegatingIOCallback<>(resp -> {
      X_Log.error("Got response! "+resp.body());
      final StringCharIterator chars = new StringCharIterator(resp.body());
      final String cursor = primitives.deserializeString(chars);
      int numResults = primitives.deserializeInt(chars);
      final ModelQueryResult<Model> result = new ModelQueryResult<>(Model.class);
      result.setCursor(cursor);
      while (numResults --> 0) {
        final String typeName = primitives.deserializeString(chars);
        final Model deserialized = deserialize(typeName, chars);
        result.addModel(deserialized);
      }
      callback.onSuccess(result);
    }, DelegatingIOCallback.failHandler(callback)));

  }

  @Override
  protected boolean isClientToServer() {
    return true;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <M extends Model> Class<M> typeToClass(final String kind) {
    return (Class<M>) typeNameToClass.get(kind);
  }

  @Override
  public MappedIterable<Method> getMethodsInDeclaredOrder(Class<?> type) {
    return MappedIterable.mapped(type.getMethods());
  }

  @Override
  public <M extends Model> Out1<M> doPersistBlocking(final String type, final M model, final long milliTimeout) {
    // TODO: use await correctly; preferably w/ an Out1 + js Promise object.
    return super.doPersistBlocking(type, model, milliTimeout);
  }
}
