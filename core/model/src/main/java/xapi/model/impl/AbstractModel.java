package xapi.model.impl;

import xapi.annotation.inject.InstanceDefault;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.fu.Out1;
import xapi.log.X_Log;
import xapi.model.X_Model;
import xapi.model.api.Model;
import xapi.model.api.ModelKey;
import xapi.model.api.NestedModel;
import xapi.model.api.PersistentModel;
import xapi.util.api.ErrorHandler;
import xapi.util.api.SuccessHandler;

import static xapi.util.impl.PairBuilder.entryOf;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Objects;

@InstanceDefault(implFor=Model.class)
public class AbstractModel implements Model, PersistentModel, NestedModel{

  protected final class Itr implements Iterable<Entry<String, Object>> {

    private final String[] keys;

    public Itr(final String[] keys) {
      this.keys = keys;
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
          Object value = getProperty(key);
          if (value == null) {
            final Class<?> type = getPropertyType(key);
            if (type.isPrimitive()) {
              value = AbstractModel.getPrimitiveValue(type);
            }
          }
          return entryOf(key, value);
        }
      };
    }
  }


  private static StringTo<Object> defaultValues = X_Collect.newStringMap(Object.class);
  protected StringTo<Object> map;
  protected Model parent;
  protected ModelKey key;

  public AbstractModel() {
    this.map = newStringMap();
  }

  public AbstractModel(final String type) {
    this.map = newStringMap();
    setKey(X_Model.newKey(type));
  }

  protected StringTo<Object> newStringMap() {
    return X_Collect.newStringMap(Object.class);
  }

  @Override
  public ModelKey getKey(){
    return key;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getProperty(final String key) {
    final Object val = map.get(key);
    if (val == null) {
      final Class<?> type = getPropertyType(key);
      return (T) getPrimitiveValue(type);
    }

    return (T) val;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getProperty(final String key, final T dflt) {
    Object val = map.get(key);
    if (val == null) {
      val = dflt;
    }
    if (val == null) {
      final Class<?> type = getPropertyType(key);
      return (T) getPrimitiveValue(type);
    } else {
      return (T) val;
    }
  }
  @Override
  public <T> T getProperty(final String key, final Out1<T> dflt) {
    Object val = map.get(key);
    if (val == null) {
      val = dflt.out1();
    }
    if (val == null) {
      final Class<?> type = getPropertyType(key);
      return (T) getPrimitiveValue(type);
    } else {
      return (T) val;
    }
  }

  @Override
  public Iterable<Entry<String, Object>> getProperties() {
    final String[] names = getPropertyNames();
    return new Itr(names);
  }

  @Override
  public Model setProperty(final String key, final Object value) {
    try {
      map.put(key, value);
    } catch (final Throwable e) {
      X_Log.error(e);
    }
    return this;
  }

  protected AbstractModel createNew() {
    return new AbstractModel();
  }
  @Override
  public Model removeProperty(final String key) {
    map.remove(key);
    return this;
  }

  @Override
  public Model child(final String propName) {
    Object existing = map.get(propName);
    assert existing == null || existing instanceof Model :
      "You requested a child model with property name "+propName+", but an object of type "
        +existing.getClass()+" already existed in this location: "+existing;
    if (existing == null){
      final AbstractModel child = createNew();//ensures subclasses can control child classes.
      child.parent = this;
      existing = child;
      map.put(propName, existing);
    }
    return (Model) existing;
  }


  @Override
  public Model parent() {
    return parent;
  }

  @Override
  public Model cache(final SuccessHandler<Model> callback) {
    X_Model.cache().cacheModel(this,callback);
    return this;
  }

  @Override
  public Model persist(final SuccessHandler<Model> callback) {
    X_Model.persist(this, callback);
    return this;
  }

  @Override
  public Model delete(final SuccessHandler<Model> callback) {
    X_Model.cache().deleteModel(this,callback);
    return this;
  }

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public Model load(final SuccessHandler<Model> callback, final boolean useCache) {
    try{
      final Model model = X_Model.cache().getModel(getKey().toString());
      callback.onSuccess(model);
    }catch(final Exception e){
      if (callback instanceof ErrorHandler) {
        ((ErrorHandler) callback).onError(e);
      }
    }
    return this;
  }

  @Override
  public Model flush() {
    return this;
  }

  @Override
  public void clear() {
    map.clear();
  }

  @Override
  public Class<?> getPropertyType(final String key) {
    throw new UnsupportedOperationException("Type "+getClass()+" does not support .getPropertyType()");
  }

  @Override
  public String[] getPropertyNames() {
    throw new UnsupportedOperationException("Type "+getClass()+" does not support .getPropertyNames()");
  }

  @Override
  public boolean equals(final Object obj) {
    return equalsForModel(this, obj);
  }

  public static boolean equalsForModel(final Model self, final Object obj) {
    if (obj instanceof Model) {
      final Model theirModel = (Model) obj;
      final ModelKey theirKey = theirModel.getKey();
      if (theirKey != null && self.getKey() != null) {
        // When both models have keys, use them for equality
        return Objects.equals(self.getKey(), theirKey);
      }
      // Otherwise, equality will do a deep check of properties.
      // It is thus recommended that if you use models as keys in a map,
      // that ALL models always have a key.
      final String[] myProps = self.getPropertyNames();
      if (!Arrays.equals(theirModel.getPropertyNames(), myProps)) {
        return false;
      }
      for(final String key : myProps) {
        if (!Objects.deepEquals(self.getProperty(key), theirModel.getProperty(key))) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return hashCodeForModel(this);
  }

  public static int hashCodeForModel(final Model m) {
    final ModelKey k = m.getKey();
    if (k == null) {
      // With a null key, the best we can do is a quick hash based on our properties
      int hash = 27;
      for (final String prop : m.getPropertyNames()) {
        final Object value = m.getProperty(prop);
        if (value == null) {
          hash += prop.hashCode();
        } else {
          final Class<?> type = m.getPropertyType(prop);
          if (type.isArray()) {
            // Close enough, we don't want to bother with deep hashing
            hash += Array.getLength(value);
          } else {
            hash += prop.hashCode();
          }
        }
      }
      return hash;
    }
    // If there is a key, always prefer using it for the hashcode.
    return k.hashCode();
  }

  @Override
  public String toString() {
    return toStringForModel(this);
  }

  public static String toStringForModel(final Model m) {
    final StringBuilder b = new StringBuilder(m.getType()+"(\n");
    if (m.getKey() != null) {
      b.append("<").append(m.getKey()).append(">: ");
    }
    for (final String name : m.getPropertyNames()) {
      final Object value = m.getProperty(name);
      b.append(name).append(": ");
      if (value != null && value.getClass().isArray()) {
        arrayToString(b, value);
      } else {
        b.append(value);
      }
      b.append('\t');
    }
    return b.append("\n)").toString();
  }
  /**
   * @param b
   * @param value
   * @return
   */
  private static void arrayToString(final StringBuilder b, final Object value) {
    b.append("[");
    for (int i = 0, m = Array.getLength(value); i < m; i++) {
      final Object child = Array.get(value, i);
      if (child != null && child.getClass().isArray()) {
        arrayToString(b, child);
      } else {
        b.append(child);
      }
      if (i < m - 1) {
        b.append(", ");
      }
    }
    b.append("]");

  }

  public static Object getPrimitiveValue(final Class<?> type) {
    if (defaultValues.isEmpty()) {
      defaultValues.put(boolean.class.getName(), false);
      defaultValues.put(byte.class.getName(), (byte)0);
      defaultValues.put(short.class.getName(), (short)0);
      defaultValues.put(char.class.getName(), '\0');
      defaultValues.put(int.class.getName(), 0);
      defaultValues.put(long.class.getName(), 0L);
      defaultValues.put(float.class.getName(), 0f);
      defaultValues.put(double.class.getName(), 0.);
    }
    return defaultValues.get(type.getName());
  }

  @Override
  public String getType() {
    if (getKey() == null) {
      throw new UnsupportedOperationException("Generic model "+getClass()+" does not have a key set. Cannot determine type.");
    }
    return getKey().getKind();
  }

  /**
   * @see xapi.model.api.Model#setKey(xapi.model.api.ModelKey)
   */
  @Override
  public Model setKey(final ModelKey key) {
    this.key = key;
    return this;
  }
}
