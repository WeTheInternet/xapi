/**
 *
 */
package xapi.jre.model;

import xapi.annotation.inject.SingletonDefault;
import xapi.constants.X_Namespace;
import xapi.fu.Out1;
import xapi.fu.In1;
import xapi.fu.has.HasName;
import xapi.log.X_Log;
import xapi.model.api.*;
import xapi.model.service.ModelService;
import xapi.model.service.ModelServiceWithRootDir;
import xapi.model.service.ModelServiceWithQueryValidation;
import xapi.model.service.ModelServiceWithCreateIfAbsent;
import xapi.platform.JrePlatform;
import xapi.prop.X_Properties;
import xapi.source.lex.StringCharIterator;
import xapi.string.X_String;
import xapi.time.X_Time;
import xapi.util.api.ErrorHandler;
import xapi.util.api.RemovalHandler;
import xapi.util.api.SuccessHandler;

import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
@JrePlatform
@SingletonDefault(implFor=ModelService.class)
public class ModelServiceJre extends AbstractJreModelService implements ModelServiceWithRootDir,
    ModelServiceWithQueryValidation, ModelServiceWithCreateIfAbsent {

  private File root;
  private volatile FileModelTransactionStore transactions;
  private boolean ioStarted;
  private static final Set<File> RESERVED_IDS = new HashSet<>();
  private final AtomicModelFile modelFiles = new AtomicModelFile();

  /**
   * Opt in before the first filesystem operation. The caller owns the already-recovered store
   * lifetime; closing it leaves this provider pinned and unusable, never silently unguarded.
   */
  public void attachTransactionStore(final FileModelTransactionStore store) throws IOException {
    java.util.Objects.requireNonNull(store, "store");
    store.readScope(() -> {
      synchronized (this) {
        if (transactions == store) return null;
        if (transactions != null || ioStarted) throw new IllegalStateException("Transaction store must attach before model IO");
        if (root != null && !root.getCanonicalFile().toPath().equals(store.directory())) {
          throw new IllegalArgumentException("Transaction store root does not match model root");
        }
        root = store.directory().toFile();
        transactions = store;
      }
      return null;
    });
  }

  public boolean hasTransactionStore() { return transactions != null; }

  /** Borrow the exact attached store; only its bootstrap owner may close or recover its lifecycle. */
  public FileModelTransactionStore transactionStore() {
    final FileModelTransactionStore store = transactions;
    if (store == null) throw new IllegalStateException("Transactional snapshots require an attached store");
    return store;
  }

  /** Pure canonical mapping; actual snapshot/commit operations validate filesystem bindings. */
  public ModelStoreKey transactionKey(final ModelKey key) throws IOException {
    return ModelFileSnapshots.key(this, transactionStore(), key);
  }

  public <M extends Model> void loadSnapshot(final ModelManifest manifest, final ModelKey key,
      final SuccessHandler<ModelFileSnapshot<M>> callback) {
    final FileModelTransactionStore store;
    try { store = transactionStore(); }
    catch (Throwable failure) { fail(callback, failure); return; }
    ModelFileSnapshots.load(this, store, manifest, key, !isClientToServer(), callback);
  }

  /** Strict wire-only decode of an original receipt/snapshot image; no hidden child reads. */
  public <M extends Model> M decodeSnapshotImage(final ModelManifest selected, final ModelKey original,
      final byte[] image) throws IOException {
    transactionStore();
    final ModelKey key = ModelKeySnapshot.complete(original);
    final ModelManifest manifest = ModelFileSnapshots.manifest(this, selected);
    return ModelFileSnapshots.decode(this, manifest, key, image.clone(), !isClientToServer());
  }

  /** Capture a canonical transaction image without incidental referenced-model saves. */
  public byte[] encodeTransactionImage(final ModelManifest manifest, final Model model) throws IOException {
    transactionStore();
    return new ModelCreateRequest(this, model == null ? null : findManifest(model.getType()), manifest, model,
        isClientToServer()).image.clone();
  }

  @Override public boolean supportsCreateIfAbsent() { return transactions != null; }

  @Override public <M extends Model> void createIfAbsent(final M model,
      final SuccessHandler<ModelCreateResult<M>> callback) {
    if (!supportsCreateIfAbsent()) { fail(callback, new UnsupportedOperationException("Conditional creation requires an attached store")); return; }
    final ModelManifest manifest;
    try { manifest = model == null ? null : findManifest(model.getType()); }
    catch (Throwable failure) { fail(callback, failure); return; }
    createIfAbsent(manifest, model, callback);
  }

  @Override public <M extends Model> void createIfAbsent(final ModelManifest manifest, final M model,
      final SuccessHandler<ModelCreateResult<M>> callback) {
    final FileModelTransactionStore store = transactions;
    if (store == null) { fail(callback, new UnsupportedOperationException("Conditional creation requires an attached store")); return; }
    final ModelManifest registered;
    try { registered = model == null ? null : findManifest(model.getType()); }
    catch (Throwable failure) { fail(callback, failure); return; }
    ModelCreateIfAbsent.create(this, store, registered, manifest, model, isClientToServer(), callback);
  }

  @Override
  protected boolean isKeyOnlyHydrationEnabled() { return transactions == null; }

  @Override
  @SuppressWarnings({
      "unchecked", "rawtypes"
  })
  protected <M extends Model> void doPersist(final String type, final M model, final SuccessHandler<M> callback) {
    ModelKey key = getKey(model, type);
    final boolean generatedId = X_String.isEmpty(key.getId());
    final File file = keyToFile(key, model, callback);
    if (file == null) {
      return;
    }
    final FileModelTransactionStore store = transactions;
    final ModelStoreKey storedKey;
    try { storedKey = store == null ? null : storeKey(store, key, file); }
    catch (Throwable failure) { releaseFailedId(key, file, generatedId); fail(callback, failure); return; }
    serialize(type, model, (serialized, error)-> {
      if (error != null) {
        releaseFailedId(key, file, generatedId);
        fail(callback, error);
        return;
      }
      final byte[] source;
      try {
        source = serialized.toSource().getBytes(StandardCharsets.UTF_8);
        if (store != null && source.length > FileModelTransactionStore.MAX_REQUEST_BYTES) {
          throw new IOException("Serialized model exceeds transaction-store record bound");
        }
      } catch (Throwable failure) {
        releaseFailedId(key, file, generatedId);
        fail(callback, failure);
        return;
      }
      final Runnable finish = () -> {
        try {
          if (store == null) modelFiles.replace(file.toPath(), source);
          else store.legacyWriteScope(storedKey, () -> { modelFiles.replace(file.toPath(), source); return null; });
        } catch (final Throwable failure) {
          if (!(failure instanceof ModelPersistenceException) && !(failure instanceof FileModelTransactionStore.RecoveryRequired)) {
            releaseFailedId(key, file, generatedId);
          }
          fail(callback, failure);
          return;
        }
        releaseId(file);
        callback.onSuccess(model);
      };
      if (isAsync()) {
        X_Time.runLater(finish);
      } else {
        finish.run();
      }
    }); // end serialize(...)
  } // end doPersist()

  private <M extends Model> File keyToFile(final ModelKey key, M model, final SuccessHandler<M> callback) {
    // no errors serializing, write our file.
    File f = getRoot(callback);
    if (f == null) {
      return f;
    }
    if (key.getNamespace().length() > 0) {
      f = new File(f, key.getNamespace());
    }
    f = new File(f, key.getKind());
    // nest hierarchical keys in a directory structure
    f = resolveParents(f, key.getParent());
    try {
      // Resolve aliases in the parent, not a final symlink destination.
      if (transactions == null) f = f.getCanonicalFile();
      else ModelFileQueries.validateDirectory(transactions.directory().toFile(), f);
    } catch (IOException failure) {
      fail(callback, failure);
      return null;
    }
    if (X_String.isEmpty(key.getId())) {
      // No id; generate one
      try {
        f = generateFile(f, model);
      } catch (final IOException e) {
        X_Log.error(ModelServiceJre.class, "Unable to save model " + model, e);
        if (callback instanceof ErrorHandler) {
          ((ErrorHandler) callback).onError(e);
        } else {
          rethrow(e);
        }
        return null;
      }
      key.setId(f.getName());
    } else {
      f = new File(f, key.getId());
    }
    if (AtomicModelFile.isTemporary(f)) {
      fail(callback, new IOException("Reserved model temporary-file name: " + f));
      return null;
    }
    return f;
  }

  private <M extends Model> ModelKey getKey(final M model, final String type) {
    ModelKey key = model.getKey();
    if (key == null) {
      key = newKey(null, type);
      model.setKey(key);
    }
    return key;
  }

  void fail(final SuccessHandler<?> callback, final Throwable e) {
    if (callback instanceof ErrorHandler) {
      ((ErrorHandler) callback).onError(e);
    } else {
      rethrow(e);
    }
  }

  @Override
  protected boolean isAsync() {
    return false;
  }

  @Override
  public <M extends Model> void load(final Class<M> modelClass, final ModelKey modelKey, final SuccessHandler<M> callback) {
    final File directory = getRoot(callback);
    if (directory == null) return;
    final FileModelTransactionStore store = transactions;
    final ModelKey captured;
    final File file;
    try {
      captured = store == null ? modelKey : captureKey(modelKey);
      file = new File(ModelFileQueries.bucket(directory, captured.getNamespace(), captured.getKind(), captured.getParent()), captured.getId());
      if (AtomicModelFile.isTemporary(file)) throw new IOException("Reserved model temporary-file name");
    } catch (Throwable failure) { fail(callback, failure); return; }
    final Out1<RemovalHandler> scope = captureScope();
    dispatchFileRead(() -> {
      final RemovalHandler handler = scope.out1();
      try {
        final M model;
        try {
          model = readScope(store, () -> {
            final byte[] source;
            if (store == null) source = Files.readAllBytes(file.toPath());
            else {
              final ModelStoreRecord record = store.read(storeKey(store, captured, file));
              if (!record.exists()) throw new NoSuchFileException(file.getPath());
              source = record.value();
            }
            final M decoded;
            try { decoded = deserialize(modelClass, new StringCharIterator(new String(source, StandardCharsets.UTF_8))); }
            catch (Throwable failure) {
              final String expectedUuid = getModelModule() == null ? null : getModelModule().getUuid();
              throw new ModelDeserializationException(expectedUuid, null, failure);
            }
            if (store != null && !keyToString(captured).equals(keyToString(decoded.getKey()))) {
              throw new IOException("Model backend returned an unexpected identity");
            }
            assert decoded.getKey().equals(captured) : "ModelKey changed during deserialization";
            decoded.setKey(captured);
            return decoded;
          });
        } catch (Throwable failure) {
          fail(callback, failure instanceof NoSuchFileException ? new ModelNotFoundException(captured) : failure);
          return;
        }
        callback.onSuccess(model);
      } finally { handler.remove(); }
    });
  }

  @SuppressWarnings({
      "unchecked", "rawtypes"
  })
  public File getRoot(final SuccessHandler<?> callback) {
    try {
      synchronized (this) {
        ioStarted = true;
        return getFilesystemRoot();
      }
    } catch (final IOException e) {
      X_Log.error(ModelServiceJre.class, "Unable to load filesystem root", e);
      if (callback instanceof ErrorHandler) {
        ((ErrorHandler) callback).onError(e);
      }
      return null;
    }
  }

  @Override
  public <M extends Model> void query(final Class<M> modelClass, final ModelQuery<M> query,
      final SuccessHandler<ModelQueryResult<M>> callback) {
    queryTyped(modelClass, getTypeName(modelClass), query, captured -> {},
        file -> decodeFile(modelClass, null, file), callback);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <M extends Model> void query(final ModelManifest manifest, final ModelQuery<M> query,
      final SuccessHandler<ModelQueryResult<M>> callback) {
    queryTyped((Class<M>) manifest.getModelType(), manifest.getType(), query, captured -> {},
        file -> decodeFile(null, manifest, file), callback);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <M extends Model> void query(final ModelManifest manifest, final ModelQuery<M> query,
      final In1<ModelQuery<M>> validation, final SuccessHandler<ModelQueryResult<M>> callback) {
    if (transactions == null) {
      fail(callback, new IllegalStateException("Validated queries require an attached transaction store"));
      return;
    }
    queryTyped((Class<M>) manifest.getModelType(), manifest.getType(), query, validation,
        file -> decodeFile(null, manifest, file), callback);
  }

  private <M extends Model> void queryTyped(final Class<M> modelClass, final String type,
      final ModelQuery<M> query, final In1<ModelQuery<M>> validation, final ModelFileQueries.Decoder<M> decoder,
      final SuccessHandler<ModelQueryResult<M>> callback) {
    if (query.getParameters().isNotEmpty()) {
      fail(callback, new UnsupportedOperationException("File-backed model queries do not support parameters"));
      return;
    }
    final File directory = getRoot(callback);
    if (directory == null) return;
    final FileModelTransactionStore store = transactions;
    final ModelQuery<M> captured;
    try { captured = captureQuery(query, store); }
    catch (Throwable failure) { fail(callback, failure); return; }
    final Out1<RemovalHandler> scope = captureScope();
    dispatchFileRead(() -> {
      final RemovalHandler handler = scope.out1();
      try {
        final ModelQueryResult<M> result;
        try {
          result = readScope(store, () -> {
            final String target = store == null ? null : captured.serialize(this, primitiveSerializer());
            validation.in(captured);
            if (target != null && !target.equals(captured.serialize(this, primitiveSerializer()))) {
              throw new IOException("Query validation changed its captured target");
            }
            if (store != null) store.validateQueryInventory(ModelFileQueries.bucket(directory,
                captured.getNamespace(), type, captured.getAncestor()).toPath(), false);
            return ModelFileQueries.typed(directory, type, modelClass, captured, store != null, decoder);
          });
        } catch (Throwable failure) {
          fail(callback, new ModelQueryFailureException(captured, directory.getPath(), failure));
          return;
        }
        callback.onSuccess(result);
      } finally { handler.remove(); }
    });
  }

  @Override
  public void query(final ModelQuery<Model> query, final SuccessHandler<ModelQueryResult<Model>> callback) {
    if (query.getParameters().isNotEmpty()) {
      fail(callback, new UnsupportedOperationException("File-backed model queries do not support parameters"));
      return;
    }
    final File directory = getRoot(callback);
    if (directory == null) return;
    final FileModelTransactionStore store = transactions;
    final ModelQuery<Model> captured;
    try { captured = captureQuery(query, store); }
    catch (Throwable failure) { fail(callback, failure); return; }
    final Out1<RemovalHandler> scope = captureScope();
    dispatchFileRead(() -> {
      final RemovalHandler handler = scope.out1();
      try {
        final ModelQueryResult<Model> result;
        try {
          result = readScope(store, () -> {
            if (store != null) store.validateQueryInventory(captured.getNamespace().isEmpty() ? directory.toPath()
                : new File(directory, captured.getNamespace()).toPath(), true);
            return ModelFileQueries.untyped(directory, captured, store != null,
                file -> decodeFile(typeToClass(file.getParentFile().getName()), null, file));
          });
        } catch (Throwable failure) {
          fail(callback, new ModelQueryFailureException(captured, directory.getPath(), failure));
          return;
        }
        callback.onSuccess(result);
      } finally { handler.remove(); }
    });
  }

  @SuppressWarnings("unchecked")
  private <M extends Model> M decodeFile(final Class<M> type, final ModelManifest manifest,
      final File file) throws IOException {
    if (transactions != null && Files.size(file.toPath()) > FileModelTransactionStore.MAX_REQUEST_BYTES) {
      throw new IOException("Query model exceeds transaction-store record bound");
    }
    final byte[] source = Files.readAllBytes(file.toPath());
    final M model = manifest == null ? deserialize(type, new StringCharIterator(new String(source, StandardCharsets.UTF_8)))
        : (M) deserialize(manifest, new StringCharIterator(new String(source, StandardCharsets.UTF_8)));
    final FileModelTransactionStore store = transactions;
    if (store != null) {
      final ModelStoreRecord record = store.read(storeKey(store, model.getKey(), file));
      if (!record.exists() || !Arrays.equals(source, record.value())) throw new IOException("Query record changed identity");
    }
    return model;
  }

  @SuppressWarnings("unchecked")
  private <M extends Model> ModelQuery<M> captureQuery(final ModelQuery<M> query,
      final FileModelTransactionStore store) throws IOException {
    if (store == null) return (ModelQuery<M>) ModelQuery.deserialize(this, primitiveSerializer(),
        new StringCharIterator(query.serialize(this, primitiveSerializer())));
    final ModelKey ancestor = query.getAncestor() == null ? null : captureKey(query.getAncestor());
    if (ancestor != null && !ancestor.getNamespace().equals(query.getNamespace())) {
      throw new IOException("Query namespace does not match its ancestor");
    }
    final ModelQuery<M> captured = (ModelQuery<M>) ModelQuery.deserialize(this, primitiveSerializer(),
        new StringCharIterator(query.serialize(this, primitiveSerializer())));
    captured.setAncestor(ancestor);
    if (captured.getAncestor() != null) {
      captured.setAncestor(captureKey(captured.getAncestor()));
      if (!captured.getNamespace().equals(captured.getAncestor().getNamespace())) {
        throw new IOException("Captured query namespace does not match its ancestor");
      }
    }
    return captured;
  }

  /** Copy original identity fields before a wire codec can inherit/normalize a child's namespace. */
  private ModelKey captureKey(final ModelKey original) throws IOException {
    return ModelKeySnapshot.complete(original);
  }

  private <T> T readScope(final FileModelTransactionStore store,
      final FileModelTransactionStore.IOOperation<T> operation) throws IOException {
    return store == null ? operation.run() : store.readScope(operation);
  }

  /** Scheduling seam; scope/gate acquisition belongs in the runnable, never around dispatch. */
  protected void dispatchFileRead(final Runnable operation) { dispatchFileOperation(operation); }

  /** Shared scheduling boundary; no monitor or callback is carried across dispatch. */
  protected void dispatchFileOperation(final Runnable operation) { X_Time.runLater(operation); }

  private File resolveParents(File file, ModelKey parent) {
    int depth = 0;
    while (parent != null) {
      if (++depth > 64) throw new IllegalArgumentException("Model ancestor depth exceeds bound");
      file = new File(file, parent.getKind());
      if (parent.isComplete()) file = new File(file, parent.getId());
      parent = parent.getParent();
    }
    return file;
  }

  /**
   * @param f
   * @return
   * @throws IOException
   */
  private File generateFile(File srcDir, Model m) throws IOException {
    final String prefix = m instanceof HasName ? ((HasName) m).getName() : "";
    synchronized (RESERVED_IDS) {
      srcDir = srcDir.getCanonicalFile();
      File[] files = srcDir.listFiles(file -> !AtomicModelFile.isTemporary(file));
      int size = files == null ? 0 : files.length;
      File candidate;
      do {
        candidate = new File(srcDir, (prefix == null ? "" : prefix + "-") + size++).getAbsoluteFile();
      } while (candidate.exists() || RESERVED_IDS.contains(candidate));
      RESERVED_IDS.add(candidate);
      m.getKey().setId(candidate.getName());
      return candidate;
    }
  }

  private static void releaseFailedId(ModelKey key, File file, boolean generatedId) {
    if (generatedId) {
      // A rejected generated ID was never committed and must not become a retry overwrite.
      key.setId(null);
      releaseId(file);
    }
  }

  private static void releaseId(File file) {
    synchronized (RESERVED_IDS) {
      // keyToFile resolves the parent canonically before allocation and serialization.
      RESERVED_IDS.remove(file);
    }
  }

  /**
   * @return
   * @throws IOException
   */
  private File getFilesystemRoot() throws IOException {
    if (root == null) {
      String modelDir = X_Properties.getProperty(X_Namespace.PROPERTY_MODEL_DIR);
      if (modelDir == null) {
        File temp;
        temp = File.createTempFile("ephemeral", "models");
        root = new File(temp.getParentFile(), "models");
        temp.delete();
      } else {
        root = new File(modelDir.replace("$USER_HOME", System.getProperty("user.home"))).getCanonicalFile();
      }
      X_Log.info(ModelServiceJre.class, "Initialized model service to ", "file://" + root);
    }
    return root;
  }

  @Override
  public void delete(final ModelKey key, final SuccessHandler<Boolean> callback) {
    final File directory = getRoot(callback);
    if (directory == null) return;
    if (X_String.isEmpty(key.getId())) throw new IllegalArgumentException("Key must have an ID to be deleted");
    final File file = new File(ModelFileQueries.bucket(directory, key.getNamespace(), key.getKind(), key.getParent()), key.getId());
    final FileModelTransactionStore store = transactions;
    final Boolean result;
    try {
      final FileModelTransactionStore.IOOperation<Boolean> remove = () -> {
        if (!Files.exists(file.toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS)) return null;
        final boolean deleted = Files.deleteIfExists(file.toPath());
        if (store != null && deleted) {
          try { modelFiles.forceDirectory(file.toPath().getParent()); }
          catch (IOException failure) { throw new ModelPersistenceException(file.toPath(), failure); }
        }
        return deleted;
      };
      result = store == null ? remove.run() : store.legacyWriteScope(storeKey(store, key, file), remove);
    } catch (Throwable failure) { fail(callback, failure); return; }
    callback.onSuccess(result);
  }

  ModelStoreKey storeKey(final FileModelTransactionStore store, final ModelKey key,
      final File file) throws IOException {
    if (key == null) throw new IOException("Missing model identity");
    int depth = 0;
    for (ModelKey part = key; part != null; part = part.getParent()) {
      if (++depth > 64 || !part.isComplete() || !key.getNamespace().equals(part.getNamespace())) {
        throw new IOException("Incomplete or inconsistent model identity");
      }
    }
    final File expected = new File(ModelFileQueries.bucket(store.directory().toFile(), key.getNamespace(),
        key.getKind(), key.getParent()), key.getId());
    if (!expected.toPath().toAbsolutePath().equals(file.toPath().toAbsolutePath())) {
      throw new IOException("Model identity does not match record location");
    }
    try {
      return new ModelStoreKey(keyToString(key), store.directory().relativize(file.toPath().toAbsolutePath()).toString());
    } catch (IllegalArgumentException invalid) { throw new IOException("Invalid file-backed model identity", invalid); }
  }

  @Override
  public synchronized void setRootDir(final File rootDir) {
    if (transactions != null) {
      try {
        if (!transactions.directory().equals(rootDir.getCanonicalFile().toPath())) {
          throw new IllegalArgumentException("Attached transaction store cannot change roots");
        }
      } catch (IOException failure) { throw new IllegalArgumentException("Cannot resolve model root", failure); }
      return;
    }
    this.root = rootDir;
  }

}
