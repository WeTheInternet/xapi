package xapi.model.api;

import java.util.Map.Entry;


public interface Model {

  //attributes
  <T> T getProperty(String key);
  <T> T getProperty(String key, T dflt);
  Iterable<Entry<String, Object>> getProperties();
  Model setProperty(String key, Object value);
  Model removeProperty(String key);
  void clear();
  ModelKey getKey();

}
