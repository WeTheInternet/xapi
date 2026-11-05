package xapi.model.service;

import xapi.model.api.Model;
import xapi.model.api.ModelCreateResult;
import xapi.model.api.ModelManifest;
import xapi.util.api.SuccessHandler;

/** Optional single-record conditional creation, independent of multi-model transactions. */
public interface ModelServiceWithCreateIfAbsent extends ModelService {
  int MAX_MODEL_BYTES = 4 * 1024 * 1024;

  /** Configured protocol support, not a promise that the current store/server is healthy or reachable. */
  boolean supportsCreateIfAbsent();

  <M extends Model> void createIfAbsent(M model, SuccessHandler<ModelCreateResult<M>> callback);

  /**
   * Requires a supplied complete key (including ancestry) and the provider's registered manifest
   * instance. Existing state is returned without replacement. New records remain unprotected for
   * legacy completion writes. UTF-8 images are bounded by MAX_MODEL_BYTES. Results are detached;
   * observers run outside the store gate and an observer exception is not a second storage error.
   *
   * The current providers snapshot only the root manifest. Nested manifests are resolved from the
   * provider's registry; register schemas at bootstrap and keep nested manifests stable until the
   * callback completes.
   *
   * An error after a write may mean creation committed. Retry the same key; never fall back to
   * ordinary persist. created=false on a retry does not imply the prior request did not commit.
   * The file provider guarantees this only among cooperating writers under its owned gate.
   */
  <M extends Model> void createIfAbsent(ModelManifest manifest, M model,
      SuccessHandler<ModelCreateResult<M>> callback);
}
