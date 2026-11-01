package xapi.jre.model;

import xapi.fu.Out1;
import xapi.model.api.Model;
import xapi.model.api.ModelCreateResult;
import xapi.model.api.ModelManifest;
import xapi.util.api.RemovalHandler;
import xapi.util.api.SuccessHandler;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/** File-provider operation lifecycle: capture/preflight, one owned decision, detached acknowledgment. */
final class ModelCreateIfAbsent {
  static <M extends Model> void create(final ModelServiceJre service, final FileModelTransactionStore store,
      final ModelManifest registered, final ModelManifest selected, final M model,
      final boolean clientToServer, final SuccessHandler<ModelCreateResult<M>> callback) {
    final ModelCreateRequest request;
    final ModelStoreKey key;
    final Out1<RemovalHandler> scope = AbstractJreModelService.captureScope();
    try {
      request = new ModelCreateRequest(service, registered, selected, model, clientToServer);
      final File file = new File(ModelFileQueries.bucket(store.directory().toFile(), request.key.getNamespace(),
          request.type, request.key.getParent()), request.key.getId());
      key = service.storeKey(store, request.key, file);
    } catch (Throwable failure) { service.fail(callback, failure); return; }
    final AtomicBoolean terminal = new AtomicBoolean();
    final Runnable operation = () -> {
      final RemovalHandler restore = scope.out1();
      try {
        final ModelCreateResult<M> result;
        try {
          final FileModelTransactionStore.CreatedImage chosen = store.createIfAbsent(key, request.image);
          final M acknowledged = request.decodeFileImage(service, chosen.image()); // Gate has been released.
          result = new ModelCreateResult<>(chosen.created, acknowledged);
        } catch (Throwable failure) {
          if (terminal.compareAndSet(false, true)) service.fail(callback, failure);
          return;
        }
        if (terminal.compareAndSet(false, true)) callback.onSuccess(result);
      } finally { restore.remove(); }
    };
    try { service.dispatchFileOperation(operation); }
    catch (Throwable failure) {
      if (terminal.compareAndSet(false, true)) service.fail(callback, failure);
      else service.rethrow(failure); // A synchronous observer failure is not a second storage failure.
    }
  }
}
