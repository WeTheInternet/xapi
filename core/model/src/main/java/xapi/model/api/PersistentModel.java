package xapi.model.api;

import xapi.util.api.SuccessHandler;

public interface PersistentModel extends Model {

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
