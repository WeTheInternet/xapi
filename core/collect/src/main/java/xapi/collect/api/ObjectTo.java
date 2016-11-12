package xapi.collect.api;

import xapi.collect.impl.EntryIterable;
import xapi.collect.proxy.CollectionProxy;
import xapi.fu.In1Out1;
import xapi.fu.MapLike;

public interface ObjectTo <K, V>
extends EntryIterable<K,V>, CollectionProxy<K,V>, HasValues<K,V>
{

  V getOrCompute(K key, In1Out1<K,V> factory);

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

  default MapLike<K, V> asMap() {
      ObjectTo<K, V> values = this;
      return new MapLike<K, V>() {
          @Override
          public int size() {
              return values.size();
          }

          @Override
          public V put(K key, V value) {
              return values.put(key, value);
          }

          @Override
          public V get(K key) {
              return values.get(key);
          }

          @Override
          public boolean has(K key) {
              return values.containsKey(key);
          }

          @Override
          public V remove(K key) {
              return values.remove(key);
          }

          @Override
          public Iterable<K> keys() {
              return values.keys();
          }
      };
  }
}
