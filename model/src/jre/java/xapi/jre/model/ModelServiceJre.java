/**
 *
 */
package xapi.jre.model;

import xapi.annotation.inject.SingletonDefault;
import xapi.constants.X_Namespace;
import xapi.dev.source.CharBuffer;
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
import xapi.time.X_Time;
import xapi.util.api.ErrorHandler;
import xapi.util.api.RemovalHandler;
import xapi.util.api.SuccessHandler;

import java.io.*;
import java.util.ArrayList;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
@JrePlatform
@SingletonDefault(implFor=ModelService.class)
public class ModelServiceJre extends AbstractJreModelService implements ModelServiceWithRootDir {

  private File root;

  @Override
  @SuppressWarnings({
      "unchecked", "rawtypes"
  })
  protected <M extends Model> void doPersist(final String type, final M model, final SuccessHandler<M> callback) {
    // For simplicity sake, lets use the file system to save our models.
    ModelKey key = getKey(model, type);
    // make sure to generate id for keys of long type w/o ids
    keyToFile(key, model, callback);
    serialize(type, model, (serialized, error)-> {
      if (error != null) {
        fail(callback, error);
        return;
      }

      final File file = keyToFile(key, model, callback);
      final Runnable finish = new Runnable() {

        @Override
        public void run() {
          try {
            if (file.exists()) {
              file.delete();
            }
            if (!file.getParentFile().isDirectory()) {
              final boolean created = file.getParentFile().mkdirs();
              if (!created) {
                X_Log.warn(ModelServiceJre.class, "Unable to create directory", file.getParentFile());
              }
            }
            final FileOutputStream result = new FileOutputStream(file);
            final String source = serialized.toSource();
            X_IO.drain(result, X_IO.toStreamUtf8(source));
            callback.onSuccess(model);
            X_Log.trace(ModelServiceJre.class, "Saved model to ", file);
            X_Log.trace(ModelServiceJre.class, "Saved model source", source);
            assert deserialize(type, CharIterator.forString(source)).equals(model);
          } catch (final IOException e) {
            X_Log.error(ModelServiceJre.class, "Unable to save model " + model, e);
            fail(callback, e);
          }
        }
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
    f.mkdirs();
    if (key.getId() == null) {
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
        return f;
      }
      key.setId(f.getName());
    } else {
      f = new File(f, key.getId());
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
    if (!f.exists()) {
      if (callback instanceof ErrorHandler) {
        ((ErrorHandler) callback).onError(new ModelNotFoundException(modelKey));
        return;
      }
    } else {
      final File file = f;
      final Out1<RemovalHandler> scope = captureScope();
      X_Time.runLater(() -> {
        final RemovalHandler handler = scope.out1();
        String result;
        try {
          result = X_IO.toStringUtf8(new FileInputStream(file));
          final M model;
          try {
            model = deserialize(modelClass, new StringCharIterator(result));
          } catch (Throwable t) {
            X_Log.error(ModelServiceJre.class, "Bad model string:\n" + result);
            final String expectedUuid = getModelModule().getUuid();
            throw new ModelDeserializationException(expectedUuid, result, t);
          }
          final ModelKey newKey = model.getKey();
          assert newKey.equals(modelKey) : "ModelKey changed during deserialization;" +
                  "\nwas: " + modelKey + "\nis: " + newKey;
          // use the key object user passed to us
          model.setKey(modelKey);
          callback.onSuccess(model);
        } catch (final Throwable e) {
          if (callback instanceof ErrorHandler) {
            X_Log.info(ModelServiceJre.class, "Unable to load file for model "+modelKey, e);
            ((ErrorHandler) callback).onError(new ModelNotFoundException(modelKey, e));
          } else {
            X_Log.error(ModelServiceJre.class, "Unable to load file for model "+modelKey, e);
            rethrow(e);
          }
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

    final ArrayList<File> files;
    final ModelQueryResult<M> result = new ModelQueryResult<>(modelClass);
    try {

      // The only query we will support is a parameterless "get all" query
      File f = getRoot(callback);
      if (query.getNamespace().length() > 0) {
        f = new File(f, query.getNamespace());
      }

      final String typeName = getTypeName(modelClass);

      f = new File(f, typeName);
      // use ancestor to create proper model hierarchy.
      ModelKey parent = query.getAncestor();
      f = resolveParents(f, parent);

      File[] allFiles;
      if (query.getCursor() == null) {
        // Yes, listing all files is not going to be very performant; however, this implementation is
        // far too naive to be used for a production system. It is primarily a proof-of-concept that can
        // be usable for developing APIs against something that is simple to use and debug
        allFiles = f.listFiles();
        if (allFiles == null) {
          allFiles = new File[]{};
        }
      } else {
        // If there is a cursor, we are continuing a query.
        allFiles = f.listFiles((dir, name) -> name.compareTo(query.getCursor()) > -1);
        if (allFiles == null) {
          // not really expected, but we'll tolerate it to avoid null pointers
          allFiles = new File[]{};
        }
      }
      final int size = Math.min(query.getPageSize(), allFiles.length);
      files = new ArrayList<File>(size);
      for (int i = 0; i < size; i++) {
        files.add(allFiles[i]);
      }
      if (size < allFiles.length) {
        result.setCursor(allFiles[size].getName());
      }
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
          fileResult = X_IO.toStringUtf8(new FileInputStream(file));
          final M model = deserialize(modelClass, new StringCharIterator(fileResult));
          fileResult = null;
          result.addModel(model);
        }
        callback.onSuccess(result);
      } catch (final Throwable t) {
        X_Log.error(ModelServiceJre.class, "Unable to load files for query ", query);
        X_Log.error(ModelServiceJre.class, "Last viewed file:", lastKnown);
        X_Log.error(ModelServiceJre.class, "Last viewed file contents:", fileResult);
        ErrorHandler.delegateTo(callback)
            .onError(t);
      } finally {
        handler.remove();
      }
    });
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

    for (final File type : f.listFiles()) {
      File[] allFiles;
      if (query.getCursor() == null) {
        // Yes, listing all files is not going to be very performant; however, this implementation is
        // far too naive to be used for a production system. It is primarily a proof-of-concept that can
        // be usable for developing APIs against something that is simple to use and debug
        allFiles = type.listFiles();
      } else {
        // If there is a cursor, we are continuing a query.
        allFiles = type.listFiles(new FilenameFilter() {

          @Override
          public boolean accept(final File dir, final String name) {
            return name.compareTo(query.getCursor()) > -1;
          }
        });
      }
      for (int i = 0, m = allFiles.length; i < m; i++) {
        if (files.size() >= query.getLimit()) {
          result.setCursor(allFiles[i].getName());
          break;
        }
        files.add(allFiles[i]);
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
            fileResult = X_IO.toStringUtf8(new FileInputStream(file));
            final Class<? extends Model> type = typeNameToClass.get(file.getParent());
            final Model model = deserialize(type, new StringCharIterator(fileResult));
            result.addModel(model);
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
  private synchronized File generateFile(File srcDir, Model m) throws IOException {
    final String prefix = m instanceof HasName ? ((HasName) m).getName() : "";
    int loopBreak = 20;
    File f;
    do {
      int size = srcDir.listFiles().length;
      f = new File(srcDir, (prefix == null ? "" : prefix) + "-" + size);
      if (loopBreak --<= 0) {
        throw new IllegalStateException("Cannot save file to " + f.getAbsolutePath() + "; check parent directory exists, is readable and executable and the disk is not out of space");
      }
      if (f.createNewFile()) {
        break;
      }
    } while (srcDir.exists());
    m.getKey().setId(f.getName());
    return f;
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
        root.mkdirs();
      } else {
        root = new File(modelDir.replace("$USER_HOME", System.getProperty("user.home"))).getCanonicalFile();
        root.mkdirs();
      }
      X_Log.info(ModelServiceJre.class, "Initialized model service to ", "file://" + root);
    }
    return root;
  }

  @Override
  public void setRootDir(final File rootDir) {
    // TODO: validate that we're not leaveing any state behind? ....kinda don't care, tbh.
    this.root = rootDir;
  }
}
