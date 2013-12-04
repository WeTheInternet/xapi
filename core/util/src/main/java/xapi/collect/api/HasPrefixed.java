package xapi.collect.api;

public interface HasPrefixed <T>{

  public Iterable<T> findPrefixed(String name);
  
}
