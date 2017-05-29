package xapi.model.impl;

import xapi.annotation.inject.SingletonDefault;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.model.X_Model;
import xapi.model.api.Model;
import xapi.model.api.ModelKey;
import xapi.model.service.ModelCache;
import xapi.util.api.SuccessHandler;

import java.util.concurrent.atomic.AtomicInteger;

@SingletonDefault(implFor=ModelCache.class)
public class AbstractModelCache implements ModelCache{

  StringTo<Model> models;
  private final AtomicInteger idNames = new AtomicInteger(0);


  public AbstractModelCache() {
    models = X_Collect.newStringMap(Model.class);
  }

  @Override
  public Model getModel(String key) {
    return models.get(key);
  }

  @Override
  public void cacheModel(Model model, SuccessHandler<Model> callback) {
    models.put(model.getKey().toString(), model);
    callback.onSuccess(model);
  }

  @Override
  public void saveModel(Model model, SuccessHandler<Model> callback) {
    models.put(model.getKey().toString(), model);
    callback.onSuccess(model);
  }

  @Override
  public void deleteModel(Model model, SuccessHandler<Model> callback) {
    models.remove(model.getKey().toString());
    callback.onSuccess(model);
  }

  @Override
  public ModelKey ensureKey(String type, Model mod) {
    return mod.getOrComputeKey(()->
        X_Model.newKey(type, "local-" + idNames.incrementAndGet())
     );
  }
}
