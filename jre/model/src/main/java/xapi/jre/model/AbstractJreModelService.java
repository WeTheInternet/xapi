package xapi.jre.model;

import xapi.collect.X_Collect;
import xapi.collect.api.ClassTo;
import xapi.collect.api.Dictionary;
import xapi.collect.api.IntTo;
import xapi.except.NotYetImplemented;
import xapi.fu.Out1;
import xapi.model.api.Model;
import xapi.model.api.ModelKey;
import xapi.model.api.ModelManifest;
import xapi.model.api.ModelManifest.MethodData;
import xapi.model.api.ModelMethodType;
import xapi.model.api.ModelModule;
import xapi.model.impl.AbstractModel;
import xapi.model.impl.AbstractModelService;
import xapi.model.impl.ModelUtil;
import xapi.reflect.X_Reflect;
import xapi.util.X_Debug;
import xapi.util.api.ConvertsTwoValues;
import xapi.util.api.ConvertsValue;
import xapi.util.api.ProvidesValue;
import xapi.util.api.RemovalHandler;

import static xapi.util.impl.PairBuilder.entryOf;

import javax.inject.Provider;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * @author James X. Nelson (james@wetheinter.net)
 * Created on 28/10/15.
 */
public abstract class AbstractJreModelService extends AbstractModelService {

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
   * This method, by default, is backed by a ThreadLocal which you can set statically via {@link #registerModule(ModelModule)}.
   *
   * Although you can override this to return something else, please be aware that this service is a global application singleton,
   * so whatever you return is going to be supplied to potentially many threads at once.
   */
  public ModelModule getModelModule() {
    return currentModule.get();
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
          return new Itr(properties, values, getDefaultValueProvider(manifest));
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
              if (method.getParameterTypes()[1] == Out1.class) {
                result = ((Out1)args[1]).out1();
              } else {
                result = args[1];
              }
            }
          }
          if (result == null) {
            return getDefaultValueProvider(manifest).convert((String)args[0]);
          }
          return result;
        case "hashCode":
          return AbstractModel.hashCodeForModel((Model) proxy);
        case "equals":
          return AbstractModel.equalsForModel((Model)proxy, args[0]);
        case "toString":
          return AbstractModel.toStringForModel((Model)proxy);
      }
      if (method.getDeclaringClass() == Model.class) {
        throw new UnsupportedOperationException("Unhandled xapi.model.api.Model method: "+method.toGenericString());
      }
      if (method.isDefault()) {
        Method original = manifest.getModelType().getMethod(method.getName(), method.getParameterTypes());
        return X_Reflect.invokeDefaultMethod(original.getDeclaringClass(), method.getName(), method.getParameterTypes(), proxy, args);
      }
      final MethodData property = manifest.getMethodData(method.getName());
      final ModelMethodType methodType = property.getMethodType(method.getName());
      if (methodType == null) {
        throw new UnsupportedOperationException("Unhandled model method: "+method.toGenericString());
      }
      switch (methodType) {
        case GET:
          Object result = values.getValue(property.getName());
          if (result == null) {
            if (method.getParameterTypes().length > 1) {
              if (args[1] instanceof Provider && !Provider.class.isAssignableFrom(property.getType())) {
                return ((Provider)args[1]).get();
              }
              try {
                // Supplier class may not be on classpath for projects < java 8
                if (args[1] instanceof Supplier && !Supplier.class.isAssignableFrom(property.getType())) {
                  return ((Provider) args[1]).get();
                }
              } catch (Throwable ignored){}
              return args[1];
            }
            return getDefaultValueProvider(manifest).convert(property.getName());
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

  protected ConvertsValue<String,Object> getDefaultValueProvider(final ModelManifest manifest) {
    return new ConvertsValue<String, Object>() {
      @Override
      public Object convert(final String from) {
        final MethodData typeData = manifest.getMethodData(from);
        if (typeData.getType().isPrimitive()) {
          return AbstractModel.getPrimitiveValue(typeData.getType());
        } else if (typeData.getType().isArray()) {
          return Array.newInstance(typeData.getType().getComponentType(), 0);
        } else {
          maybeInitDefaults(defaultValueProvider);
          // Handle other default values
          final ConvertsTwoValues<ModelManifest, MethodData, Object> provider = defaultValueProvider.get(typeData.getType());
          if (provider != null) {
            return provider.convert(manifest, typeData);
          }
        }
        return null;
      }
    };
  }

  protected void maybeInitDefaults(ClassTo<ConvertsTwoValues<ModelManifest, MethodData, Object>> defaultValueProvider) {
    if (defaultValueProvider.isEmpty()) {
      defaultValueProvider.put(IntTo.class, new ConvertsTwoValues<ModelManifest, MethodData, Object>() {
            @Override
            public Object convert(ModelManifest manifest, MethodData method) {
              final Class[] types = method.getTypeParams();
              assert types.length == 1 : "Expected exactly one type argument for IntTo instances";
              return X_Collect.newList(types[0]);
            }
          });
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

  private final ClassTo<ProvidesValue<Object>> modelFactories;
  private final ClassTo<ConvertsTwoValues<ModelManifest, MethodData, Object>> defaultValueProvider;
  private final ClassTo<ModelManifest> modelManifests;

  @SuppressWarnings("unchecked")
  protected AbstractJreModelService() {
    modelManifests = X_Collect.newClassMap(ModelManifest.class);
    modelFactories = X_Collect.newClassMap(Class.class.cast(ProvidesValue.class));
    defaultValueProvider = X_Collect.newClassMap(Class.class.cast(ConvertsTwoValues.class));
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
          return (M) Proxy.newProxyInstance(
              Thread.currentThread().getContextClassLoader(),
              new Class<?>[]{modelClass}, newInvocationHandler(modelClass)
          );
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
    final ModelModule module = getModelModule();
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

  @Override
  @SuppressWarnings("unchecked")
  public <M extends Model> Class<M> typeToClass(final String kind) {
    return (Class<M>) typeNameToClass.get(kind);
  }

  protected void rethrow(Exception e) {
    X_Debug.rethrow(e);
  }
}
