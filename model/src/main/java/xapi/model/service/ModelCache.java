package xapi.model.service;

import xapi.model.api.Model;
import xapi.model.api.ModelKey;
import xapi.util.api.SuccessHandler;

import static xapi.model.X_Model.keyToString;

public interface ModelCache {

  Model getModel(String key);

  default Model getModel(ModelKey key) {
    return getModel(keyToString(key));
  }
  void cacheModel(Model model, SuccessHandler<Model> callback);
  void saveModel(Model model, SuccessHandler<Model> callback);
  void deleteModel(Model model, SuccessHandler<Model> callback);

  ModelKey ensureKey(String type, Model mod);
}
