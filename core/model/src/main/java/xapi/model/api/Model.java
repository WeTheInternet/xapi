package xapi.model.api;

import xapi.fu.In1;
import xapi.fu.In1Out1;
import xapi.fu.Out1;

import java.util.Map.Entry;


public interface Model {

  //attributes
  <T> T getProperty(String key);
  <T> T getProperty(String key, T dflt);
  <T> T getProperty(String key, Out1<T> dflt);

  default <T> T getOrSaveProperty(String key, Out1<T> dflt) {
    boolean save = hasProperty(key);
    T val = getProperty(key, dflt);
    if (save) {
      setProperty(key, val);
    }
    return val;
  }

  default <T> T getOrCreate(Out1<T> getter, Out1<T> factory, In1<T> setter) {
    T is = getter.out1();
    if (is == null) {
      is = factory.out1();
      setter.in(is);
    }
    return is;
  }

  default <T> T compute(Out1<T> getter, In1Out1<T, T> mapper, In1<T> setter) {
    T was = getter.out1();
    final T is = mapper.io(was);
    if (was != is) {
      setter.in(is);
    }
    return is;
  }

  boolean hasProperty(String key);

  Class<?> getPropertyType(String key);
  Iterable<Entry<String, Object>> getProperties();
  String[] getPropertyNames();
  Model setProperty(String key, Object value);
  Model removeProperty(String key);
  void clear();
  ModelKey getKey();
  String getType();
  Model setKey(ModelKey key);

}
