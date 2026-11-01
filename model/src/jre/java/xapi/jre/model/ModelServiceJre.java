/**
 *
 */
package xapi.jre.model;

import xapi.annotation.inject.SingletonDefault;
import xapi.constants.X_Namespace;
import xapi.fu.Out1;
import xapi.fu.has.HasName;
import xapi.io.X_IO;
import xapi.log.X_Log;
import xapi.model.api.*;
import xapi.model.api.ModelQuery.QueryParameter;
import xapi.model.service.ModelService;
import xapi.model.service.ModelServiceWithRootDir;
import xapi.platform.JrePlatform;
import xapi.prop.X_Properties;
import xapi.source.lex.CharIterator;
import xapi.source.lex.StringCharIterator;
import xapi.string.X_String;
import xapi.time.X_Time;
import xapi.util.api.ErrorHandler;
import xapi.util.api.RemovalHandler;
import xapi.util.api.SuccessHandler;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
public class ModelServiceJre extends AbstractJreModelService implements ModelServiceWithRootDir {

  private static final Comparator<? super File> FILE_SORT = (a, b)->a.getName().compareTo(b.getName());
  private File root;
  private static final Set<File> RESERVED_IDS = new HashSet<>();
  private final AtomicModelFile modelFiles = new AtomicModelFile();

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
    serialize(type, model, (serialized, error)-> {
      if (error != null) {
        releaseFailedId(key, file, generatedId);
        fail(callback, error);
        return;
      }
      final byte[] source;
      try {
        source = serialized.toSource().getBytes(StandardCharsets.UTF_8);
      } catch (Throwable failure) {
        releaseFailedId(key, file, generatedId);
        fail(callback, failure);
        return;
      }
      final Runnable finish = () -> {
        try {
          modelFiles.replace(file.toPath(), source);
        } catch (final IOException failure) {
          if (!(failure instanceof ModelPersistenceException)) {
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
      f = f.getCanonicalFile();
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

  private void fail(final SuccessHandler<?> callback, final Throwable e) {
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

  @SuppressWarnings("unchecked")
  @Override
  public <M extends Model> void load(final Class<M> modelClass, final ModelKey modelKey, final SuccessHandler<M> callback) {
    File f = getRoot(callback);
    if (f == null) {
      return;
    }
    if (modelKey.getNamespace().length() > 0) {
      f = new File(f, modelKey.getNamespace());
    }
    f = new File(f, modelKey.getKind());
    f = resolveParents(f, modelKey.getParent());
    // TODO: use nested structure for hierarchical keys
    f = new File(f, modelKey.getId());
    if (AtomicModelFile.isTemporary(f)) {
      fail(callback, new IOException("Reserved model temporary-file name: " + f));
      return;
    }
    {
      final File file = f;
      final Out1<RemovalHandler> scope = captureScope();
      X_Time.runLater(() -> {
        final RemovalHandler handler = scope.out1();
        try {
          final M model;
          try {
            String result = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            try {
              model = deserialize(modelClass, new StringCharIterator(result));
            } catch (Throwable failure) {
              final String expectedUuid = getModelModule() == null ? null : getModelModule().getUuid();
              // Do not attach the raw file buffer here; decoder diagnostics still propagate.
              throw new ModelDeserializationException(expectedUuid, null, failure);
            }
            final ModelKey newKey = model.getKey();
            assert newKey.equals(modelKey) : "ModelKey changed during deserialization";
            model.setKey(modelKey);
          } catch (final Throwable failure) {
            fail(callback, failure instanceof NoSuchFileException
                ? new ModelNotFoundException(modelKey) : failure);
            return;
          }
          // A consumer callback failure is not a failed storage read.
          callback.onSuccess(model);
        } finally {
          handler.remove();
        }
      });
    }
  }

  @SuppressWarnings({
      "unchecked", "rawtypes"
  })
  public File getRoot(final SuccessHandler<?> callback) {
    try {
      return getFilesystemRoot();
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
    if (query.getParameters().isNotEmpty()) {
      ErrorHandler.delegateTo(callback)
          .onError(new UnsupportedOperationException("The basic, file-backed "+getClass().getName()+" does not support any complex queries"));
      return;
    }

    final ModelQueryResult<M> result = new ModelQueryResult<>(modelClass);
    final ArrayList<File> files;
    try {
      files = queryFiles(getTypeName(modelClass), query, result, callback);
    } catch (Throwable t) {
      ErrorHandler.delegateTo(callback)
          .onError(t);
      return;
    }

    final Out1<RemovalHandler> scope = captureScope();
    X_Time.runLater(() -> {
      final RemovalHandler handler = scope.out1();
      File lastKnown = null;
      String fileResult = null;
      try {
        for (final File file : files) {
          lastKnown = file;
          fileResult = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
          final M model = deserialize(modelClass, new StringCharIterator(fileResult));
          fileResult = null;
          result.addModel(model);
        }
        callback.onSuccess(result);
      } catch (final Throwable t) {
        failQuery(t, query, lastKnown, fileResult, callback);
      } finally {
        handler.remove();
      }
    });
  }

  private <M extends Model> void failQuery(Throwable t, final ModelQuery<M> query, final File lastKnown, final String fileResult, final SuccessHandler<ModelQueryResult<M>> callback) {
    X_Log.error(ModelServiceJre.class, "Unable to load files for query ", query);
    X_Log.error(ModelServiceJre.class, "Last viewed file:", lastKnown);
    // Keep corrupt payloads on disk, out of logs.
    ErrorHandler.delegateTo(callback)
            .onError(new ModelQueryFailureException(query, lastKnown.getPath(), t));
  }

  @Override
  public <M extends Model> void query(final ModelManifest manifest, final ModelQuery<M> query,
      final SuccessHandler<ModelQueryResult<M>> callback) {
    if (query.getParameters().isNotEmpty()) {
      ErrorHandler.delegateTo(callback)
          .onError(new UnsupportedOperationException("The basic, file-backed "+getClass().getName()+" does not support any complex queries"));
      return;
    }

    final ModelQueryResult<M> result = new ModelQueryResult<>((Class)manifest.getModelType());
    final ArrayList<File> files;
    try {
      files = queryFiles(manifest.getType(), query, result, callback);
    } catch (Throwable t) {
      ErrorHandler.delegateTo(callback)
              .onError(t);
      return;
    }

    final Out1<RemovalHandler> scope = captureScope();
    X_Time.runLater(() -> {
      final RemovalHandler handler = scope.out1();
      File lastKnown = null;
      String fileResult = null;
      try {
        for (final File file : files) {
          lastKnown = file;
          fileResult = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
          final M model = deserialize(manifest, new StringCharIterator(fileResult));
          fileResult = null;
          result.addModel(model);
        }
        callback.onSuccess(result);
      } catch (final Throwable t) {
        failQuery(t, query, lastKnown, fileResult, callback);
      } finally {
        handler.remove();
      }
    });
  }

  private <M extends Model> ArrayList<File> queryFiles(final String typeName, final ModelQuery<M> query, final ModelQueryResult<M> result, final SuccessHandler<ModelQueryResult<M>> callback) {
      ArrayList<File> files;

      // The only query we will support is a parameterless "get all" query
      File f = getRoot(callback);
      if (query.getNamespace().length() > 0) {
        f = new File(f, query.getNamespace());
      }

      f = new File(f, typeName);
      // use ancestor to create proper model hierarchy.
      ModelKey parent = query.getAncestor();
      f = resolveParents(f, parent);

      File[] allFiles;
      if (query.getCursor() == null) {
        // Yes, listing all files is not going to be very performant; however, this implementation is
        // far too naive to be used for a production system. It is primarily a proof-of-concept that can
        // be usable for developing APIs against something that is simple to use and debug
        allFiles = f.listFiles(AtomicModelFile::isModelFile);
        if (allFiles == null) {
          allFiles = new File[]{};
        }
      } else {
        // If there is a cursor, we are continuing a query.
        allFiles = f.listFiles(file -> AtomicModelFile.isModelFile(file)
            && file.getName().compareTo(query.getCursor()) > -1);
        if (allFiles == null) {
          // not really expected, but we'll tolerate it to avoid null pointers
          allFiles = new File[]{};
        }
      }
      Arrays.sort(allFiles, FILE_SORT);
      final int size = Math.min(query.getPageSize(), allFiles.length);
      files = new ArrayList<File>(size);
      for (int i = 0; i < size; i++) {
        files.add(allFiles[i]);
      }
      if (size < allFiles.length) {
        result.setCursor(allFiles[size].getName());
      }
      return files;
  }

  private File resolveParents(File f, ModelKey parent) {
    while (parent != null) {
      f = new File(f, parent.getKind());
      if (parent.isComplete()) {
        f = new File(f, parent.getId());
      }
      parent = parent.getParent();
    }
    return f;
  }

  @Override
  @SuppressWarnings("rawtypes")
  public void query(final ModelQuery<Model> query, final SuccessHandler<ModelQueryResult<Model>> callback) {
    for (final QueryParameter param : query.getParameters()) {
      throw new UnsupportedOperationException("The basic, file-backed "+getClass().getName()+" does not support any complex queries");
    }
    // The only query we will support is a parameterless "get all" query
    // This implementation generally sucks, and only exists for very basic usage.
    // If a file-backed API is truly desired, one should be implemented using proper
    // indexing, filtering, sorting, etc. And it should use java.nio.File...

    File f = getRoot(callback);
    if (query.getNamespace().length() > 0) {
      f = new File(f, query.getNamespace());
    }

    final ArrayList<File> files = new ArrayList<File>();
    final ModelQueryResult<Model> result = new ModelQueryResult<>(null);

    final File[] typeList = f.listFiles();
    File[] allFiles = null;
    if (typeList != null) {
        for (final File type : typeList) {
          if (query.getCursor() == null) {
            // Yes, listing all files is not going to be very performant; however, this implementation is
            // far too naive to be used for a production system. It is primarily a proof-of-concept that can
            // be usable for developing APIs against something that is simple to use and debug
            allFiles = type.listFiles(AtomicModelFile::isModelFile);
          } else {
            // If there is a cursor, we are continuing a query.
            allFiles = type.listFiles(new FilenameFilter() {

              @Override
              public boolean accept(final File dir, final String name) {
                return AtomicModelFile.isModelFile(new File(dir, name))
                    && name.compareTo(query.getCursor()) > -1;
              }
            });
          }
          if (allFiles != null) {
            Arrays.sort(allFiles, FILE_SORT);
          }
          for (int i = 0, m = allFiles == null ? 0 : allFiles.length; i < m; i++) {
            if (files.size() >= query.getLimit()) {
              result.setCursor(allFiles[i].getName());
              break;
            }
            if (allFiles[i].isFile()) {
              files.add(allFiles[i]);
            } else {
              // for now, ignoring ancestor keys. it's too ugly
//              final File[] childFiles = allFiles[i].listFiles();
//              if (childFiles != null) {
//                for (File childFile : childFiles) {
//                  files.add(childFile);
//                }
//              }
            }
          }
        }
    }

    final Out1<RemovalHandler> scope = captureScope();
    X_Time.runLater(new Runnable() {

      @Override
      public void run() {
        final RemovalHandler handler = scope.out1();
        String fileResult;
        try {
          for (final File file : files) {
            final Class<? extends Model> type = typeNameToClass.get(file.getParentFile().getName());
            if (file.isFile()) {
              fileResult = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
              final Model model = deserialize(type, new StringCharIterator(fileResult));
              result.addModel(model);
            } else {
              final File[] kids = file.listFiles();
              if (kids != null) {
                for (File kid : kids) {
                  fileResult = new String(Files.readAllBytes(kid.toPath()), StandardCharsets.UTF_8);
                  final Model model = deserialize(type, new StringCharIterator(fileResult));
                  result.addModel(model);
                }

              }
            }
          }
          callback.onSuccess(result);
        } catch (final Exception e) {
          X_Log.error(ModelServiceJre.class, "Unable to load files for query "+query);
          if (callback instanceof ErrorHandler) {
            ((ErrorHandler) callback).onError(new RuntimeException("Unable to load files for query "+query));
          } else {
            rethrow(e);
          }
        } finally {
          handler.remove();
        }
      }
    });
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
    File f = getRoot(callback);
    if (f == null) {
      callback.onSuccess(null);
      return;
    }
    if (X_String.isEmpty(key.getId())) {
      throw new IllegalArgumentException("Key must have an ID to be deleted: " + key);
    }
    if (!key.getNamespace().isEmpty()) {
      f = new File(f, key.getNamespace());
    }
    f = new File(f, key.getKind());
    // nest hierarchical keys in a directory structure
    f = resolveParents(f, key.getParent());
    f = new File(f, key.getId());
    if (!f.exists()) {
      callback.onSuccess(null);
      return;
    }
    final boolean success;
    try {
      success = f.delete();
    } catch (Exception e) {
      if (callback instanceof ErrorHandler) {
        ErrorHandler.delegateTo(callback).onError(e);
      } else {
        X_Log.warn(ModelServiceJre.class, "Problem deleting model", key, e);
        callback.onSuccess(false);
      }
      return;
    }
    callback.onSuccess(success);
  }

  @Override
  public void setRootDir(final File rootDir) {
    // TODO: validate that we're not leaveing any state behind? ....kinda don't care, tbh.
    this.root = rootDir;
  }

}
