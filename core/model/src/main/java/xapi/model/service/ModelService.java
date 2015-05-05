package xapi.model.service;

import xapi.model.api.Model;
import xapi.util.api.SuccessHandler;

public interface ModelService {

  void register(Class<? extends Model> model);
  <T extends Model> T create(Class<T> key);
  void persist(Model model, SuccessHandler<Model> callback);
  <M extends Model> String serialize(final Class<M> cls, final M model);
  <M extends Model> M deserialize(final Class<M> cls, final String model);


}
