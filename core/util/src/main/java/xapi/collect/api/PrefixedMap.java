package xapi.collect.api;

public interface PrefixedMap <T> extends HasPrefixed<T> {

  T get(String key);
  void put(String key, T value);
  
}
