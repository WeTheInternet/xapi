package xapi.jre.model;

import xapi.fu.Out1;
import xapi.model.api.*;
import xapi.util.api.RemovalHandler;
import xapi.util.api.SuccessHandler;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/** Owned file identity, codec and asynchronous snapshot lifecycle; no domain rules or store ownership. */
final class ModelFileSnapshots {
  static ModelStoreKey key(final ModelServiceJre service, final FileModelTransactionStore store,
      final ModelKey original) throws IOException {
    final ModelKey key = ModelKeySnapshot.complete(original);
    final File file = new File(ModelFileQueries.bucket(store.directory().toFile(), key.getNamespace(),
        key.getKind(), key.getParent()), key.getId());
    return service.storeKey(store, key, file);
  }

  static ModelManifest manifest(final ModelServiceJre service, final ModelManifest selected) {
    return ModelCreateRequest.freezeManifest(service, selected == null ? null : service.findManifest(selected.getType()), selected);
  }

  static <M extends Model> M decode(final ModelServiceJre service, final ModelManifest manifest,
      final ModelKey key, final byte[] image, final boolean receivingDirection) throws IOException {
    if (!manifest.getType().equals(key.getKind())) throw new IOException("Snapshot manifest does not match full key");
    return ModelCreateRequest.decodeImage(service, manifest, manifest.getType(), service.keyToString(key), image, receivingDirection);
  }

  static <M extends Model> void load(final ModelServiceJre service, final FileModelTransactionStore store,
      final ModelManifest selected, final ModelKey original, final boolean receivingDirection,
      final SuccessHandler<ModelFileSnapshot<M>> callback) {
    final ModelKey key;
    final ModelStoreKey target;
    final ModelManifest manifest;
    final Out1<RemovalHandler> scope = AbstractJreModelService.captureScope();
    try {
      key = ModelKeySnapshot.complete(original); target = key(service, store, key);
      manifest = manifest(service, selected);
      if (!manifest.getType().equals(key.getKind())) throw new IOException("Snapshot manifest does not match full key");
    } catch (Throwable failure) { service.fail(callback, failure); return; }
    final AtomicBoolean terminal = new AtomicBoolean();
    final Runnable operation = () -> {
      final RemovalHandler restore = scope.out1();
      try {
        final ModelFileSnapshot<M> result;
        try {
          final ModelStoreRecord record = store.read(target);
          final M model = record.exists() ? decode(service, manifest, key, record.value(), receivingDirection) : null;
          result = new ModelFileSnapshot<>(target, record, model);
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
      else service.rethrow(failure);
    }
  }
}
