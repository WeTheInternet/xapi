package xapi.collect.impl;

import xapi.fu.Lazy;

import java.util.LinkedHashMap;

/**
 * A LazyMap, backed by a LinkedHashMap.
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 * @param <K>
 * @param <V>
 */
public class LazyLinkedMap <K, V> extends Lazy<LinkedHashMap<K, V>> {

  public LazyLinkedMap() {
    super(LinkedHashMap::new);
  }
}
