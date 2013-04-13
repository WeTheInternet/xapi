package xapi.model.api;

import java.util.Map;

import xapi.util.api.SuccessHandler;

public interface Model {

  //attributes
  <T> T getProperty(String key);
  <T> T getProperty(String key, T dflt);
  Map<String, Object> getProperties();
  Model setProperty(String key, Object value);
  Model removeProperty(String key);
  void clear();

  //nested types
  Model child(String propName);
  Model parent();
  ModelKey getKey();

  //persistence
  Model cache(SuccessHandler<Model> callback);
  Model persist(SuccessHandler<Model> callback);
  Model delete(SuccessHandler<Model> callback);
  Model load(SuccessHandler<Model> callback, boolean useCache);
  /**
   * Anything cached will be persisted,
   * and on any platforms which can block, this method will block until
   * all persistence operations have completed.

   * @return this, for chaining
   */
  Model flush();

}
