package xapi.model;

import xapi.annotation.compile.MagicMethod;
import xapi.bytecode.NotFoundException;
import xapi.fu.In1;
import xapi.fu.In1Out1;
import xapi.inject.X_Inject;
import xapi.model.api.Model;
import xapi.model.api.ModelKey;
import xapi.model.api.ModelManifest;
import xapi.model.api.ModelQuery;
import xapi.model.api.ModelQueryResult;
import xapi.model.service.ModelCache;
import xapi.model.service.ModelService;
import xapi.source.impl.StringCharIterator;
import xapi.util.api.ErrorHandler;
import xapi.util.api.SuccessHandler;

import javax.inject.Provider;

public class X_Model {

  private X_Model() {}

  private static final Provider<ModelCache> cache = X_Inject.singletonLazy(ModelCache.class);
  private static final Provider<ModelService> service = X_Inject.singletonLazy(ModelService.class);

  public static ModelCache cache(){
    return cache.get();
  }

  @MagicMethod(doNotVisit=true,
      documentation="This magic method generates the model class and all of its dependent models, "
        + "then re-routes to the same provider as X_Inject.instance()")
  public static <M extends Model, Generic extends M> M create(final Class<Generic> modelClass) {
    return service.get().create(modelClass);
  }

  public static Model create() {
    return create(Model.class);
  }

  @MagicMethod(doNotVisit=true,
      documentation="This magic method generates the model class, and returns the internal table name of the modelClass")
  public static <M extends Model> String register(final Class<M> modelClass) {
    return service.get().register(modelClass);
  }

  public static <M extends Model> void persist(final M model, final SuccessHandler<M> callback) {
    // TODO: return a Promises-like object
    service.get().persist(model, callback);
  }

  public static <M extends Model> void mutate(final Class<M> type, ModelKey key, In1Out1<M, M> mutator, SuccessHandler<M> callback) {
    // TODO route back errors... force an error callback?
    load(type, key, SuccessHandler.handler(item->{
      if (item == null) {
        ErrorHandler.delegateTo(callback)
            .onError(new NotFoundException("No entity exists with key " + key));
      } else {
        final M mutated = mutator.io(item);
        persist(mutated, callback);
      }
    }, ErrorHandler.delegateTo(callback)));
  }

  public static <M extends Model> void load(final Class<M> modelClass, final ModelKey modelKey, final SuccessHandler<M> callback) {
    // TODO: return a Promises-like object
    service.get().load(modelClass, modelKey, callback);
  }

  public static <M extends Model> String serialize(final Class<M> cls, final M model) {
    return service.get().serialize(cls, model).toString();
  }

  public static <M extends Model> M deserialize(final Class<M> cls, final String model) {
    return service.get().deserialize(cls, new StringCharIterator(model));
  }

  public static <M extends Model> String serialize(final ModelManifest manifest, final M model) {
    return service.get().serialize(manifest, model).toString();
  }

  public static <M extends Model> M deserialize(final ModelManifest manifest, final String model) {
    return service.get().deserialize(manifest, new StringCharIterator(model));
  }

  public static ModelService getService() {
    return service.get();
  }

  public static String keyToString(final ModelKey key) {
    return service.get().keyToString(key);
  }

  public static ModelKey keyFromString(final String key) {
    return service.get().keyFromString(key);
  }

  public static ModelKey newKey(final String kind) {
    return service.get().newKey("", kind);
  }

  public static ModelKey newKey(final String namespace, final String kind) {
    return service.get().newKey(namespace, kind);
  }

  public static ModelKey newKey(final String namespace, final String kind, final String id) {
    return service.get().newKey(namespace, kind, id);
  }

  public static <M extends Model> void query(final Class<M> modelClass, final ModelQuery<M> query, final SuccessHandler<ModelQueryResult<M>> callback) {
    assert modelClass != null : "A typed query -must- supply a modelClass";
    service.get().query(modelClass, query, callback);
  }

  public static void queryAll(final ModelQuery<Model> query, final SuccessHandler<ModelQueryResult<Model>> callback) {
    service.get().query(query, callback);
  }

}
