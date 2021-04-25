package xapi.collect.prefixed;

import xapi.fu.itr.MappedIterable;

public interface HasPrefixed <T>{

  MappedIterable<T> findPrefixed(String name);

}
