package xapi.collect.impl;

import java.util.LinkedHashMap;

import xapi.inject.impl.SingletonProvider;

/**
 * A LazyMap, backed by a LinkedHashMap.
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 * @param <K>
 * @param <V>
 */
public class LazyLinkedMap <K, V> extends SingletonProvider<LinkedHashMap<K, V>>{
  @Override
  protected LinkedHashMap<K, V> initialValue() {
    return new LinkedHashMap<K, V>();
  }
}