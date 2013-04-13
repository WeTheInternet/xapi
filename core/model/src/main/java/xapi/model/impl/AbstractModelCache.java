package xapi.model.impl;

import xapi.annotation.inject.SingletonDefault;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.model.api.Model;
import xapi.model.service.ModelCache;
import xapi.util.api.SuccessHandler;

@SingletonDefault(implFor=ModelCache.class)
public class AbstractModelCache implements ModelCache{

  StringTo<Model> models;


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

}
