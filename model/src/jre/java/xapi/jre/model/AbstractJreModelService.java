package xapi.jre.model;

import xapi.bytecode.impl.BytecodeAdapterService;
import xapi.collect.X_Collect;
import xapi.collect.api.ClassTo;
import xapi.collect.api.Dictionary;
import xapi.collect.api.IntTo;
import xapi.except.NotYetImplemented;
import xapi.fu.*;
import xapi.fu.data.MapLike;
import xapi.fu.data.SetLike;
import xapi.fu.itr.MappedIterable;
import xapi.fu.java.X_Jdk;
import xapi.model.api.*;
import xapi.model.api.ModelManifest.MethodData;
import xapi.model.impl.AbstractModel;
import xapi.model.impl.AbstractModelService;
import xapi.model.impl.ModelUtil;
import xapi.reflect.X_Reflect;
import xapi.debug.X_Debug;
import xapi.util.api.RemovalHandler;
import xapi.util.api.SuccessHandler;

import javax.inject.Provider;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Supplier;

import static xapi.util.impl.PairBuilder.entryOf;

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

  public static Out1<RemovalHandler> captureScope() {
    final ModelModule module = currentModule.get();
    return () -> {
      final ModelModule was = currentModule.get();
      currentModule.set(module);
      return () -> {
        if (module == currentModule.get()) {
          if (was == null) {
            currentModule.remove();
          } else {
            currentModule.set(was);
          }
        }
      };
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
    final MapLike<String, In3<String, Object, Object>> callbacks;
    final SetLike<In3<String, Object, Object>> globalChange;
    ModelKey key;

    public ModelInvocationHandler(final Class<? extends Model> modelClass) {
      this(modelClass, X_Collect.newDictionaryInsertionOrdered());
    }

    public ModelInvocationHandler(final Class<? extends Model> modelClass, final Dictionary<String, Object> values) {
      this(getOrMakeModelManifest(modelClass), values);
    }

    public ModelInvocationHandler(final ModelManifest manifest, final Dictionary<String, Object> values) {
      this.manifest = manifest;
      this.values = values;
      this.callbacks = X_Jdk.mapHash();
      this.globalChange = X_Jdk.setLinked();
    }

    @Override
    public String toString() {
      return "ModelInvocationHandler{" +
              "type=" + manifest.getType() +
              ", key=" + key +
              ", knownValues=" + values.getKeys().join(",") +
              '}';
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
      switch (method.getName()) {
        case "setProperty":
          if (method.getParameterTypes().length == 2) {
            String key = (String)args[0];
            Object oldVal = values.setValue(key, args[1]);
            if (!Objects.equals(oldVal, args[1])) {
              invokeCallbacks(key, oldVal, args[1]);
            }
            return proxy;
          }
        case "setKey":
          key = (ModelKey) args[0];
          return proxy;
        case "removeProperty":
          if (method.getParameterTypes().length == 1) {
            values.removeValue((String)args[0]);
            return proxy;
          }
        case "getType":
          return manifest.getType();
        case "onChange":
          callbacks.computeValue((String)args[0], was-> {
              In3 asIn3 = args[1] instanceof In2 ? ((In2)args[1]).ignore1() :
                      args[1] instanceof In3 ? (In3)args[1] : null;
              if (asIn3 != null) {
                  return was == null ? asIn3 : was.useAfterMe(asIn3);
              }
//              if (args[1] instanceof In2) {
//                  return was == null ? asIn3 : was.useAfterMe(asIn3);
//              }
//              if (args[1] instanceof In3) {
//                return was == null ? (In3)args[1] : was.useAfterMe((In3)args[1]);
//              }
              throw new IllegalArgumentException("Illegal onChange method argument " + args[1] + " not castable to In2; use In2.ignoreAll() instead of null");
          });
          return proxy;
        case "fireChangeEvent":
          final String keyName = (String) args[0];
          final Object was = args[1], is = args[2];
          if (was != is) {
            invokeCallbacks(keyName, was, is);
          }
          return null;
        case "onGlobalChange":
            //noinspection rawtypes
            final In3 handler = (In3) args[0];
            //noinspection unchecked
            this.globalChange.add(handler);
            //noinspection unchecked
          return Do.of(()->globalChange.remove(handler));
        case "getPropertyType":
          final String name = (String) args[0];
          return manifest.getMethodData(name).getType();
        case "getPropertyNames":
          return manifest.getPropertyNames();
        case "getProperties":
          final String[] properties = manifest.getPropertyNames();
          return new Itr(properties, values, getDefaultValueProvider(manifest, values::setValue));
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
            return getDefaultValueProvider(manifest, values::setValue).io((String)args[0]);
          }
          return result;
        case "hashCode":
          return AbstractModel.hashCodeForModel((Model) proxy);
        case "equals":
          return AbstractModel.equalsForModel((Model)proxy, args[0]);
        case "toString":
          return AbstractModel.toStringForModel((Model)proxy);
      }
      if (method.isDefault()) {
        Method original = manifest.getModelType().getMethod(method.getName(), method.getParameterTypes());
        return X_Reflect.invokeDefaultMethod(original.getDeclaringClass(), method.getName(), method.getParameterTypes(), proxy, args);
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
              } catch (ClassCastException ignored){}
              return args[1];
            }
            return getDefaultValueProvider(manifest, values::setValue).io(property.getName());
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
              invokeCallbacks(property.getName(), result, args[1]);
              if (returnsBoolean) {
                return true;
              }
            }
            if (returnsBoolean) {
              return false;
            }
          } else {
            result = values.setValue(property.getName(), args[0]);
            if (!Objects.equals(result, args[0])) {
              invokeCallbacks(property.getName(), result, args[0]);
            }
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

    private void invokeCallbacks(final String key, final Object was, final Object is) {
      assert was == null ? is != null : !was.equals(is) : "Should not call invokeCallbacks for key " + key + " with equal values: " + was + " == " + is;
      callbacks.getMaybe(key).readIfPresent(cb->{
        cb.in(key, was, is);
      });
        for (In3<String, Object, Object> callback : globalChange) {
            callback.in(key, was, is);
        }
    }


  }

  protected In1Out1<String,Object> getDefaultValueProvider(final ModelManifest manifest, In2<String, Object> setter) {
    return from -> {
      final MethodData typeData = manifest.getMethodData(from);
      if (typeData.getType().isPrimitive()) {
        return AbstractModel.getPrimitiveValue(typeData.getType());
      } else if (typeData.getType().isArray()) {
        final Object arr = Array.newInstance(typeData.getType().getComponentType(), 0);
        setter.in(from, arr);
        return arr;
      } else {
        maybeInitDefaults(defaultValueProvider);
        // Handle other default values
        final In2Out1<ModelManifest, MethodData, Object> provider = defaultValueProvider.get(typeData.getType());
        if (provider != null) {
          final Object val = provider.io(manifest, typeData);
          setter.in(from, val);
          return val;
        }
      }
      return null;
    };
  }

  protected void maybeInitDefaults(ClassTo<In2Out1<ModelManifest, MethodData, Object>> defaultValueProvider) {
    if (defaultValueProvider.isEmpty()) {
      defaultValueProvider.put(IntTo.class, (manifest, method) -> {
          final Class[] types = method.getTypeParams();
          assert types.length == 1 : "Expected exactly one type argument for IntTo instances";
          return X_Collect.newList(types[0]);
      });
    }
  }

  private final class Itr implements MappedIterable<Entry<String, Object>> {

    private final String[] keys;
    private final Dictionary<String, Object> map;
    private final In1Out1<String, Object> defaultValueProvider;

    private Itr(final String[] keys, final Dictionary<String, Object> map, final In1Out1<String, Object> defaultValueProvider) {
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
          final String key = keys[pos++];
          Object value = map.getValue(key);
          if (value == null) {
            value = defaultValueProvider.io(key);
          }
          return entryOf(key, value);
        }
      };
    }

  }

  private final ClassTo<Out1<Object>> modelFactories;
  private final ClassTo<In2Out1<ModelManifest, MethodData, Object>> defaultValueProvider;
  private final ClassTo<ModelManifest> modelManifests;

  @SuppressWarnings("unchecked")
  protected AbstractJreModelService() {
    modelManifests = X_Collect.newClassMap(ModelManifest.class);
    modelFactories = X_Collect.newClassMap(Class.class.cast(Out1.class));
    defaultValueProvider = X_Collect.newClassMap(Class.class.cast(In2Out1.class));
  }


  /**
   * @see xapi.model.impl.AbstractModelService#create(java.lang.Class)
   */
  @SuppressWarnings({
      "unchecked", "rawtypes"
  })
  @Override
  public <M extends Model> M doCreate(final Class<M> key) {
    Out1 factory = modelFactories.get(key);
    if (factory == null) {
      factory = createModelFactory(key);
      modelFactories.put(key, factory);
    }
    M model = (M)factory.out1();

    // JRE can do proper reflective registration on demand...
    if (!classToTypeName.containsKey(key)) {
      register(key);
    }
    return model;
  }

  @SuppressWarnings("unchecked")
  protected <M extends Model> Out1<M> createModelFactory(final Class<M> modelClass) {
    // TODO: check for an X_Inject interface definition and prefer that, if possible...
    if (modelClass.isInterface()) {
      return ()-> (M) Proxy.newProxyInstance(
              Thread.currentThread().getContextClassLoader(),
              new Class<?>[]{modelClass}, newInvocationHandler(modelClass)
          );

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
    return (Class<M>) typeNameToClass.getOrCreate(kind, k->
      modelManifests.findAndReduce((cls, manifest)->{
        if (manifest.getType().equals(kind)) {
          return (Class<M>)cls;
        }
        return null;
      })
    );
  }

  @Override
  public ModelManifest findManifest(final Class<?> type) {
    return modelManifests.get(type);
  }

  @Override
  protected ModelManifest findManifest(final String type) {
    return modelManifests.get(typeToClass(type));
  }

  protected void rethrow(Throwable e) {
    X_Debug.rethrow(e);
  }

  @Override
  public MappedIterable<Method> getMethodsInDeclaredOrder(Class<?> type) {
    return BytecodeAdapterService.getMethodsInDeclaredOrder(type);
  }

  @Override
  public void flushCaches() {
    super.flushCaches();
    this.modelManifests.clear();
    this.defaultValueProvider.clear();
    this.modelFactories.clear();
  }

  @Override
  public void registerManifest(final ModelManifest manifest) {
    modelManifests.put(manifest.getModelType(), manifest);
  }
}
