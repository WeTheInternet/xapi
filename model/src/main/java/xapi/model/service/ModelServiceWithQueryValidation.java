package xapi.model.service;

import xapi.model.api.Model;
import xapi.model.api.ModelManifest;
import xapi.model.api.ModelQuery;
import xapi.model.api.ModelQueryResult;
import xapi.util.api.SuccessHandler;
import xapi.fu.In1;

/** Optional capability for validation and the actual query to share one synchronous read scope. */
public interface ModelServiceWithQueryValidation extends ModelService {

  /**
   * Runs validation immediately before query enumeration/read/deserialization, inside the provider's
   * read scope. The validator receives the exact captured query used for enumeration, never the
   * caller's mutable original. Validation must be synchronous, must not alter that target or mutate
   * storage, and must not wait for asynchronous work. The result/error callback runs after that
   * scope is released and may initiate another call.
   */
  <M extends Model> void query(ModelManifest manifest, ModelQuery<M> query,
      In1<ModelQuery<M>> validation, SuccessHandler<ModelQueryResult<M>> callback);
}
