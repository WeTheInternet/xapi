package xapi.model.api;

import javax.inject.Provider;
import java.util.Map.Entry;


public interface Model {

  //attributes
  <T> T getProperty(String key);
  <T> T getProperty(String key, T dflt);
  <T> T getProperty(String key, Provider<T> dflt);
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
