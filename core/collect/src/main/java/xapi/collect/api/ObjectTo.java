package xapi.collect.api;

import xapi.collect.impl.EntryIterable;
import xapi.collect.proxy.CollectionProxy;
import xapi.util.api.ConvertsValue;

public interface ObjectTo <K, V>
extends EntryIterable<K,V>, CollectionProxy<K,V>, HasValues<K,V>
{

  V getOrCompute(K key, ConvertsValue<K,V> factory);

  interface Many <K, V> extends ObjectTo<K, IntTo<V>> {
    default boolean add (K key, V value) {
      return get(key).add(value);
    }
  }

// Inherited from CollectionProxy
//  V get(K key);
//  boolean remove(K key);

  V put(K key, V value);

  Class<?> componentType();

}
