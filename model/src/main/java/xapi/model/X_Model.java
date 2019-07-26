package xapi.model;

import xapi.annotation.compile.MagicMethod;
import xapi.except.NoSuchItem;
import xapi.fu.In1Out1;
import xapi.fu.Lazy;
import xapi.fu.Out1;
import xapi.fu.itr.MappedIterable;
import xapi.inject.X_Inject;
import xapi.model.api.*;
import xapi.model.service.ModelCache;
import xapi.model.service.ModelService;
import xapi.source.impl.StringCharIterator;
import xapi.util.api.ErrorHandler;
import xapi.util.api.SuccessHandler;

import java.lang.reflect.Method;

public class X_Model {

  private X_Model() {}

  private static final Lazy<ModelCache> cache = X_Inject.singletonLazy(ModelCache.class);
  private static final Lazy<ModelService> service = X_Inject.singletonLazy(ModelService.class);

  public static ModelCache cache(){
    return cache.out1();
  }

  @MagicMethod(doNotVisit=true,
      documentation="This magic method generates the model class and all of its dependent models, "
        + "then re-routes to the same provider as X_Inject.instance()")
  public static <M extends Model, Generic extends M> M create(final Class<Generic> modelClass) {
    return service.out1().create(modelClass);
  }

  public static Model create() {
    return create(Model.class);
  }

  @MagicMethod(doNotVisit=true,
      documentation="This magic method generates the model class, and returns the internal table name of the modelClass")
  public static <M extends Model> String register(final Class<M> modelClass) {
    return service.out1().register(modelClass);
  }

  public static <M extends Model> void persist(final M model, final SuccessHandler<M> callback) {
    // TODO: return a Promises-like object
    service.out1().persist(model, callback);
  }

  public static <M extends Model> void upsert(final Class<M> type, ModelKey key, In1Out1<M, M> mutator, Out1<M> creator, SuccessHandler<M> callback) {
    mutate(type, key, mutator, SuccessHandler.handler(callback, error->{
      if (error instanceof NoSuchItem) {
        final M value = creator.out1();
        persist(value, callback);
      } else {
        ErrorHandler.delegateTo(callback)
            .onError(error);
      }
    }));
  }
  public static <M extends Model> void mutate(final Class<M> type, ModelKey key, In1Out1<M, M> mutator, SuccessHandler<M> callback) {
    // TODO route back errors... force an error callback?
    load(type, key, SuccessHandler.handler(item->{
      if (item == null) {
        ErrorHandler.delegateTo(callback)
            .onError(new NoSuchItem(key));
      } else {
        final M mutated = mutator.io(item);
        persist(mutated, callback);
      }
    }, ErrorHandler.delegateTo(callback)));
  }

  public static <M extends Model> void load(final Class<M> modelClass, final ModelKey modelKey, final SuccessHandler<M> callback) {
    // TODO: return a Promises-like object
    service.out1().load(modelClass, modelKey, callback);
  }

  public static <M extends Model> String serialize(final Class<M> cls, final M model) {
    return service.out1().serialize(cls, model).toSource();
  }

  public static <M extends Model> M deserialize(final Class<M> cls, final String model) {
    return service.out1().deserialize(cls, new StringCharIterator(model));
  }

  public static <M extends Model> String serialize(final ModelManifest manifest, final M model) {
    return service.out1().serialize(manifest, model).toSource();
  }

  public static <M extends Model> M deserialize(final ModelManifest manifest, final String model) {
    return service.out1().deserialize(manifest, new StringCharIterator(model));
  }

  public static ModelService getService() {
    return service.out1();
  }

  public static String keyToString(final ModelKey key) {
    return service.out1().keyToString(key);
  }

  public static ModelKey keyFromString(final String key) {
    return service.out1().keyFromString(key);
  }

  public static ModelKey newKey(final String kind) {
    return service.out1().newKey("", kind);
  }

  public static ModelKey newKey(final String namespace, final String kind) {
    return service.out1().newKey(namespace, kind);
  }

  public static ModelKey newKey(final String namespace, final String kind, final String id) {
    return service.out1().newKey(namespace, kind, id);
  }

  public static <M extends Model> void query(final Class<M> modelClass, final ModelQuery<M> query, final SuccessHandler<ModelQueryResult<M>> callback) {
    assert modelClass != null : "A typed query -must- supply a modelClass";
    service.out1().query(modelClass, query, callback);
  }

  public static void queryAll(final ModelQuery<Model> query, final SuccessHandler<ModelQueryResult<Model>> callback) {
    service.out1().query(query, callback);
  }

  public static MappedIterable<Method> getMethodsInDeclaredOrder(Class<?> type) {
    return service.out1().getMethodsInDeclaredOrder(type);
  }

  public static ModelKey ensureKey(String type, Model mod) {
    return cache().ensureKey(type, mod);
  }
}
