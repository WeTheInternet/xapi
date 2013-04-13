package xapi.model.service;

import xapi.model.api.Model;
import xapi.util.api.SuccessHandler;

public interface ModelService {

  <T extends Model> T create(Class<T> key);
  void persist(Model model, SuccessHandler<Model> callback);


}
