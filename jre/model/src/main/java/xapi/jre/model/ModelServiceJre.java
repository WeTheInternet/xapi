/**
 *
 */
package xapi.jre.model;

import xapi.annotation.inject.SingletonDefault;
import xapi.dev.source.CharBuffer;
import xapi.io.X_IO;
import xapi.log.X_Log;
import xapi.model.api.Model;
import xapi.model.api.ModelKey;
import xapi.model.api.ModelNotFoundException;
import xapi.model.api.ModelQuery;
import xapi.model.api.ModelQuery.QueryParameter;
import xapi.model.api.ModelQueryResult;
import xapi.model.service.ModelService;
import xapi.platform.JrePlatform;
import xapi.source.api.CharIterator;
import xapi.source.impl.StringCharIterator;
import xapi.time.X_Time;
import xapi.util.api.ErrorHandler;
import xapi.util.api.ProvidesValue;
import xapi.util.api.RemovalHandler;
import xapi.util.api.SuccessHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
@JrePlatform
@SingletonDefault(implFor=ModelService.class)
public class ModelServiceJre extends AbstractJreModelService {

  private File root;

  @Override
  @SuppressWarnings({
      "unchecked", "rawtypes"
  })
  protected <M extends Model> void doPersist(final String type, final M model, final SuccessHandler<M> callback) {
    // For simplicity sake, lets use the file system to save our models.
    ModelKey key = model.getKey();
    if (key == null) {
      key = newKey(null, type);
      model.setKey(key);
    }
    File f = getRoot(callback);
    if (f == null) {
      return;
    }
    if (key.getNamespace().length() > 0) {
      f = new File(f, key.getNamespace());
    }
    f = new File(f, key.getKind());
    f.mkdirs();
    if (key.getId() == null) {
      // No id; generate one
      try {
        f = generateFile(f);
      } catch (final IOException e) {
        X_Log.error(getClass(), "Unable to save model "+model, e);
        if (callback instanceof ErrorHandler) {
          ((ErrorHandler) callback).onError(e);
        } else {
          rethrow(e);
        }
        return;
      }
      key.setId(f.getName());
    } else {
      f = new File(f, key.getId());
    }
    final CharBuffer serialized = serialize(type, model);
    final File file = f;
    final Runnable finish = new Runnable() {

      @Override
      public void run() {
        try {
          if (file.exists()) {
            file.delete();
          }
          final FileOutputStream result = new FileOutputStream(file);
          final String source = serialized.toSource();
          X_IO.drain(result, X_IO.toStreamUtf8(source));
          callback.onSuccess(model);
          X_Log.info(getClass(), "Saved model to ", file);
          assert deserialize(type, CharIterator.forString(source)).equals(model);
        } catch (final IOException e) {
          X_Log.error(getClass(), "Unable to save model " + model, e);
          if (callback instanceof ErrorHandler) {
            ((ErrorHandler) callback).onError(e);
          } else {
            rethrow(e);
          }
        }
      }
    };
    if (isAsync()) {
      X_Time.runLater(finish);
    } else {
      finish.run();
    }
  }

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
    f = new File(f, modelKey.getId());
    if (!f.exists()) {
      if (callback instanceof ErrorHandler) {
        ((ErrorHandler) callback).onError(new ModelNotFoundException(modelKey));
        return;
      }
    } else {
      final File file = f;
      final ProvidesValue<RemovalHandler> scope = captureScope();
      X_Time.runLater(new Runnable() {

        @Override
        public void run() {
          final RemovalHandler handler = scope.get();
          String result;
          try {
            result = X_IO.toStringUtf8(new FileInputStream(file));
            final M model;
            try {
              model = deserialize(modelClass, new StringCharIterator(result));
            } catch (Throwable t) {
              X_Log.error(ModelServiceJre.class, "Bad model string:\n" + result);
              throw t;
            }
            callback.onSuccess(model);
          } catch (final Throwable e) {
            if (callback instanceof ErrorHandler) {
              X_Log.trace(ModelServiceJre.class, "Unable to load file for model "+modelKey, e);
              ((ErrorHandler) callback).onError(new ModelNotFoundException(modelKey));
            } else {
              X_Log.error(ModelServiceJre.class, "Unable to load file for model "+modelKey, e);
              rethrow(e);
            }
          } finally {
            handler.remove();
          }
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
      X_Log.error(getClass(), "Unable to load filesystem root", e);
      if (callback instanceof ErrorHandler) {
        ((ErrorHandler) callback).onError(e);
      }
      return null;
    }
  }

  @Override
  public <M extends Model> void query(final Class<M> modelClass, final ModelQuery<M> query,
      final SuccessHandler<ModelQueryResult<M>> callback) {
    for (final QueryParameter param : query.getParameters()) {
      throw new UnsupportedOperationException("The basic, file-backed "+getClass().getName()+" does not support any complex queries");
    }
    // The only query we will support is a parameterless "get all" query

    File f = getRoot(callback);
    if (query.getNamespace().length() > 0) {
      f = new File(f, query.getNamespace());
    }

    final String typeName = getTypeName(modelClass);

    f = new File(f, typeName);

    File[] allFiles;
    if (query.getCursor() == null) {
      // Yes, listing all files is not going to be very performant; however, this implementation is
      // far too naive to be used for a production system. It is primarily a proof-of-concept that can
      // be usable for developing APIs against something that is simple to use and debug
      allFiles = f.listFiles();
    } else {
      // If there is a cursor, we are continuing a query.
      allFiles = f.listFiles(new FilenameFilter() {

        @Override
        public boolean accept(final File dir, final String name) {
          return name.compareTo(query.getCursor()) > -1;
        }
      });
    }
    final int size = Math.min(query.getPageSize(), allFiles.length);
    final ArrayList<File> files = new ArrayList<File>(size);
    for (int i = 0; i < size; i++) {
      files.add(allFiles[i]);
    }
    final ModelQueryResult<M> result = new ModelQueryResult<>(modelClass);
    if (size < allFiles.length) {
      result.setCursor(allFiles[size].getName());
    }
    allFiles = null;

    final ProvidesValue<RemovalHandler> scope = captureScope();
    X_Time.runLater(new Runnable() {

      @Override
      public void run() {
        final RemovalHandler handler = scope.get();
        String fileResult;
        try {
          for (final File file : files) {
            fileResult = X_IO.toStringUtf8(new FileInputStream(file));
            final M model = deserialize(modelClass, new StringCharIterator(fileResult));
            result.addModel(model);
          }
          callback.onSuccess(result);
        } catch (final Exception e) {
          X_Log.error(getClass(), "Unable to load files for query "+query);
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

    final ProvidesValue<RemovalHandler> scope = captureScope();
    X_Time.runLater(new Runnable() {

      @Override
      public void run() {
        final RemovalHandler handler = scope.get();
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
          X_Log.error(getClass(), "Unable to load files for query "+query);
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
  private synchronized File generateFile(File f) throws IOException {
    final int size = f.listFiles().length;
    f = new File(f, Integer.toString(size));
    f.createNewFile();
    return f;
  }

  /**
   * @return
   * @throws IOException
   */
  private File getFilesystemRoot() throws IOException {
    if (root == null) {
      File temp;
      temp = File.createTempFile("ephemeral", "models");
      root = new File(temp.getParentFile(), "models");
      temp.delete();
      root.mkdirs();
    }
    return root;
  }

}
