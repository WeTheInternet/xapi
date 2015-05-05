package xapi.model;

import javax.inject.Provider;

import xapi.annotation.gwt.MagicMethod;
import xapi.inject.X_Inject;
import xapi.model.api.Model;
import xapi.model.service.ModelCache;
import xapi.model.service.ModelService;
import xapi.util.api.SuccessHandler;

public class X_Model {

  private X_Model() {}

  private static final Provider<ModelCache> cache = X_Inject.singletonLazy(ModelCache.class);
  private static final Provider<ModelService> service = X_Inject.singletonLazy(ModelService.class);

  public static ModelCache cache(){
    return cache.get();
  }

  @MagicMethod(doNotVisit=true, documentation="This magic method re-routes to the same provider as X_Inject.instance()")
  public static <M extends Model> M create(final Class<M> modelClass) {
    return service.get().create(modelClass);
  }

  public static void persist(final Model model, final SuccessHandler<Model> callback) {
    // TODO: return a Promises-like object
    service.get().persist(model, callback);
  }

  public static <M extends Model> String serialize(final Class<M> cls, final M model) {
    return service.get().serialize(cls, model);
  }

  public static <M extends Model> M deserialize(final Class<M> cls, final String model) {
    return service.get().deserialize(cls, model);
  }

}
