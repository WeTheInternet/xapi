package xapi.collect.api;

import xapi.fu.MappedIterable;

public interface HasPrefixed <T>{

  MappedIterable<T> findPrefixed(String name);

}
