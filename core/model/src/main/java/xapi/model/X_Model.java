package xapi.model;

import javax.inject.Provider;

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

  public static <M extends Model> M create(Class<M> modelClass) {
    return service.get().create(modelClass);
  }

  public static void persist(Model model, SuccessHandler<Model> callback) {
    // TODO: return a Future-like object
    service.get().persist(model, callback);
  }

}
