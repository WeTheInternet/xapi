/**
 *
 */
package xapi.jre.model;

import static xapi.util.impl.PairBuilder.entryOf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Objects;

import xapi.annotation.inject.SingletonDefault;
import xapi.collect.X_Collect;
import xapi.collect.api.ClassTo;
import xapi.collect.api.Dictionary;
import xapi.dev.source.CharBuffer;
import xapi.except.NotYetImplemented;
import xapi.io.X_IO;
import xapi.log.X_Log;
import xapi.model.api.Model;
import xapi.model.api.ModelKey;
import xapi.model.api.ModelManifest;
import xapi.model.api.ModelManifest.MethodData;
import xapi.model.api.ModelMethodType;
import xapi.model.api.ModelModule;
import xapi.model.api.ModelNotFoundException;
import xapi.model.impl.AbstractModel;
import xapi.model.impl.AbstractModelService;
import xapi.model.impl.ModelUtil;
import xapi.model.service.ModelService;
import xapi.platform.JrePlatform;
import xapi.source.impl.StringCharIterator;
import xapi.time.X_Time;
import xapi.util.api.ConvertsValue;
import xapi.util.api.ErrorHandler;
import xapi.util.api.ProvidesValue;
import xapi.util.api.RemovalHandler;
import xapi.util.api.SuccessHandler;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
@JrePlatform
@SingletonDefault(implFor=ModelService.class)
public class ModelServiceJre extends AbstractModelService {

  private static ThreadLocal<ModelModule> currentModule = new ThreadLocal<>();

  public static RemovalHandler registerModule(final ModelModule module) {
    currentModule.set(module);
    return new RemovalHandler() {
      @Override
      public void remove() {
        currentModule.remove();
      }
    };
  }

  public static ProvidesValue<RemovalHandler> captureScope() {
    final ModelModule module = currentModule.get();
    return new ProvidesValue<RemovalHandler>() {
      @Override
      public RemovalHandler get() {
        final ModelModule was = currentModule.get();
        currentModule.set(module);
        return new RemovalHandler() {
          @Override
          public void remove() {
            if (module == currentModule.get()) {
              if (was == null) {
                currentModule.remove();
              } else {
                currentModule.set(was);
              }
            }
          }
        };
      }
    };
  }

  /**
   * @author James X. Nelson (james@wetheinter.net, @james)
   *
   */
  public class ModelInvocationHandler implements InvocationHandler {

    final ModelManifest manifest;
    final Dictionary<String, Object> values;
    ModelKey key;

    public ModelInvocationHandler(final Class<? extends Model> modelClass) {
      this(modelClass, X_Collect.newDictionary());
    }

    public ModelInvocationHandler(final Class<? extends Model> modelClass, final Dictionary<String, Object> values) {
      this(getOrMakeModelManifest(modelClass), values);
    }

    public ModelInvocationHandler(final ModelManifest manifest, final Dictionary<String, Object> values) {
      this.manifest = manifest;
      this.values = values;
    }

      @Override
          public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            switch (method.getName()) {
              case "setProperty":
                if (method.getParameterTypes().length == 2) {
                  values.setValue((String)args[0], args[1]);
                  return proxy;
                }
              case "setKey":
                key = (ModelKey) args[0];
                return proxy;
              case "removeProperty":
                if (method.getParameterTypes().length == 1) {
                  values.removeValue((String)args[0]);
                  return this;
                }
              case "getType":
                return manifest.getType();
              case "getPropertyType":
                final String name = (String) args[0];
                return manifest.getMethodData(name).getType();
              case "getPropertyNames":
                return manifest.getPropertyNames();
              case "getProperties":
                final String[] properties = manifest.getPropertyNames();
                return new Itr(properties, values, new ConvertsValue<String, Object>() {
                  @Override
                  public Object convert(final String from) {
                    final MethodData typeData = manifest.getMethodData(from);
                    if (typeData.getType().isPrimitive()) {
                      return AbstractModel.getPrimitiveValue(typeData.getType());
                    }
                    return null;
                  }
                });
              case "getKey":
                return key;
              case "clear":
                values.clearValues();
                return proxy;
              case "getProperty":
                Object result = null;
                if (method.getParameterTypes().length == 1) {
                  // no default value
                  result = values.getValue((String)args[0]);
                } else if (method.getParameterTypes().length == 2) {
                  // there is a default value...
                  result = values.getValue((String)args[0]);
                  if (result == null) {
                    result = args[1];
                  }
                }
                if (result == null) {
                  final MethodData typeData = manifest.getMethodData((String)args[0]);
                  return AbstractModel.getPrimitiveValue(typeData.getType());
                }
                return result;
              case "hashCode":
                return AbstractModel.hashCodeForModel((Model)proxy);
              case "equals":
                return AbstractModel.equalsForModel((Model)proxy, args[0]);
              case "toString":
                return AbstractModel.toStringForModel((Model)proxy);
            }
            if (method.getDeclaringClass() == Model.class) {
              throw new UnsupportedOperationException("Unhandled xapi.model.api.Model method: "+method.toGenericString());
            }
            final MethodData property = manifest.getMethodData(method.getName());
            final ModelMethodType methodType = property.getMethodType(method.getName());
            if (methodType == null) {
              throw new UnsupportedOperationException("Unhandled model method: "+method.toGenericString());
            }
            switch (methodType) {
              case GET:
                Object result = values.getValue(property.getName());
                if (result == null && method.getParameterTypes().length > 1) {
                  return args[1];
                }
                return result;
              case SET:
                boolean isFluent = ModelUtil.isFluent(method);
                result = null;
                if (method.getParameters().length == 2) {
                  // This is a check-and-set
                  final Object previous = values.getValue(property.getName());
                  final boolean returnsBoolean = method.getReturnType() == boolean.class;
                  if (Objects.equals(previous, args[0])) {
                    result = values.setValue(property.getName(), args[1]);
                    if (returnsBoolean) {
                      return true;
                    }
                  }
                  if (returnsBoolean) {
                    return false;
                  }
                } else {
                  result = values.setValue(property.getName(), args[0]);
                }
                if (isFluent) {
                  return proxy;
                }
                if (method.getReturnType() == null || method.getReturnType() == void.class) {
                  return null;
                }
                return result;
              case ADD:
              case ADD_ALL:
              case CLEAR:
                throw new NotYetImplemented("Method "+method.toGenericString()+" of "+
                    method.getDeclaringClass()+" is not yet implemented");
              case REMOVE:
                result = null;
                isFluent = ModelUtil.isFluent(method);
                if (method.getParameters().length == 2) {
                  // This is a check-and-remove
                  final Object previous = values.getValue(property.getName());
                  final boolean returnsBoolean = method.getReturnType() == boolean.class;
                  if (Objects.equals(previous, args[0])) {
                    result = values.removeValue(property.getName());
                    if (returnsBoolean) {
                      return true;
                    }
                  }
                  if (returnsBoolean) {
                    return false;
                  }
                } else {
                  result = values.removeValue(property.getName());
                }
                if (isFluent) {
                  return proxy;
                }
                if (method.getReturnType() == null || method.getReturnType() == void.class) {
                  return null;
                }
                return result;
            }
            return null;
          }

  }

  private final class Itr implements Iterable<Entry<String, Object>> {

    private final String[] keys;
    private final Dictionary<String, Object> map;
    private final ConvertsValue<String, Object> defaultValueProvider;

    private Itr(final String[] keys, final Dictionary<String, Object> map, final ConvertsValue<String, Object> defaultValueProvider) {
      this.keys = keys;
      this.map = map;
      this.defaultValueProvider = defaultValueProvider;
    }

    @Override
    public Iterator<Entry<String, Object>> iterator() {
      return new Iterator<Entry<String,Object>>() {

        int pos = 0;
        @Override
        public boolean hasNext() {
          return pos < keys.length;
        }

        @Override
        public Entry<String, Object> next() {
          final String key = keys[pos];
          Object value = map.getValue(key);
          if (value == null) {
            value = defaultValueProvider.convert(key);
          }
          return entryOf(key, value);
        }
      };
    }

  }

  @SuppressWarnings("unchecked")
  private final ClassTo<ProvidesValue<Object>> modelFactories = X_Collect.newClassMap(
      Class.class.cast(ProvidesValue.class)
  );
  private final ClassTo<ModelManifest> modelManifests = X_Collect.newClassMap(ModelManifest.class);
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
    File f;
    try {
      f = getFilesystemRoot();
    } catch (final IOException e) {
      X_Log.error(getClass(), "Unable to load filesystem root", e);
      if (callback instanceof ErrorHandler) {
        ((ErrorHandler) callback).onError(e);
      }
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
        }
        return;
      }
      key.setId(f.getName());
    } else {
      f = new File(f, key.getId());
    }
    final CharBuffer serialized = serialize(type, model);
    final File file = f;
    X_Time.runLater(new Runnable() {

      @Override
      public void run() {
        try {
          if (file.exists()) {
            file.delete();
          }
          final FileOutputStream result = new FileOutputStream(file);
          X_IO.drain(result, X_IO.toStreamUtf8(serialized.toString()));
          callback.onSuccess(model);
          X_Log.info(getClass(), "Saved model to ",file);
        } catch (final IOException e) {
          X_Log.error(getClass(), "Unable to save model "+model, e);
          if (callback instanceof ErrorHandler) {
            ((ErrorHandler) callback).onError(e);
          }
        }
      }
    });
  }

  @SuppressWarnings("unchecked")
  @Override
  public <M extends Model> void load(final Class<M> modelClass, final ModelKey modelKey, final SuccessHandler<M> callback) {
    File f;
    try {
      f = getFilesystemRoot();
    } catch (final IOException e) {
      X_Log.error(getClass(), "Unable to load filesystem root", e);
      if (callback instanceof ErrorHandler) {
        ((ErrorHandler) callback).onError(e);
      }
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
            final M model = deserialize(modelClass, new StringCharIterator(result));
            callback.onSuccess(model);
          } catch (final Exception e) {
            X_Log.error(getClass(), "Unable to load file for model "+modelKey);
            if (callback instanceof ErrorHandler) {
              ((ErrorHandler) callback).onError(new ModelNotFoundException(modelKey));
            }
          } finally {
            handler.remove();
          }
        }
      });
    }
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

  /**
   * @see xapi.model.impl.AbstractModelService#create(java.lang.Class)
   */
  @SuppressWarnings({
      "unchecked", "rawtypes"
  })
  @Override
  public <M extends Model> M create(final Class<M> key) {
    ProvidesValue factory = modelFactories.get(key);
    if (factory == null) {
      factory = createModelFactory(key);
      modelFactories.put(key, factory);
    }
    return (M)factory.get();
  }

  protected <M extends Model> ProvidesValue<M> createModelFactory(final Class<M> modelClass) {
    // TODO: check for an X_Inject interface definition and prefer that, if possible...
    if (modelClass.isInterface()) {
      return new ProvidesValue<M>() {

        @Override
        @SuppressWarnings("unchecked")
        public M get() {
          return (M) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
              new Class<?>[]{modelClass}, newInvocationHandler(modelClass));
        }
      };

    } else {
      // The type is not an interface.  We are boned.
      throw new NotYetImplemented("Unable to generate class provider for " + modelClass+"; "
          + "only interface types are supported at this time");
    }
  }

  protected InvocationHandler newInvocationHandler(final Class<? extends Model> modelClass) {
    return new ModelInvocationHandler(modelClass);
  }

  protected ModelManifest getOrMakeModelManifest(final Class<? extends Model> cls) {
    final ModelModule module = currentModule.get();
    if (module != null) {
      final String typeName = getTypeName(cls);
      return module.getManifest(typeName);
    }
    ModelManifest manifest = modelManifests.get(cls);
    if (manifest == null) {
      manifest = ModelUtil.createManifest(cls);
      modelManifests.put(cls, manifest);
    }
    return manifest;
  }

}
