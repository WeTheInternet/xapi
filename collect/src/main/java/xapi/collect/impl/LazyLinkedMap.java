package xapi.collect.impl;

import xapi.fu.Lazy;

import java.util.LinkedHashMap;

///
/// A LazyMap, backed by a LinkedHashMap.
/// @author "James X. Nelson (james@wetheinter.net)"
///
/// @param <K> The type of the key
/// @param <V> The type of the value
///
public class LazyLinkedMap <K, V> extends Lazy<LinkedHashMap<K, V>> {

  public LazyLinkedMap() {
    super(LinkedHashMap::new);
  }
}
