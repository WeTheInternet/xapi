package xapi.model.api;

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
