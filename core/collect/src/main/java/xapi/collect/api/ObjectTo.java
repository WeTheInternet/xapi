package xapi.collect.api;

import xapi.collect.proxy.CollectionProxy;
import xapi.fu.Filter.Filter1;
import xapi.fu.Filter.Filter2;
import xapi.fu.In1;
import xapi.fu.In1Out1;
import xapi.fu.MapLike;
import xapi.fu.Maybe;
import xapi.fu.iterate.SizedIterable;
import xapi.fu.java.EntryIterable;

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
          public void clear() {
              values.clear();
          }

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
          public SizedIterable<K> keys() {
              return values.keys();
          }
      };
  }


    default void forEachWhereKey(Filter1<K> keyFilter, In1<V> callback) {
        for (K key : keys()) {
            if (keyFilter.filter1(key)) {
                final V value = get(key);
                callback.in(value);
            }
        }
    }

    default Maybe<V> firstWhereKey(Filter1<K> key) {
        Maybe<V> result = Maybe.not();
        for (K cls : keys()) {
            if (key.filter1(cls)) {
                final V value = get(cls);
                result = Maybe.nullable(value);
                if (result.isPresent()) {
                    return result;
                }
            }
        }
        return result;
    }

    default Maybe<V> firstWhereValue(Filter1<V> key) {
        Maybe<V> result = Maybe.not();
        for (V value : values()) {
            if (key.filter1(value)) {
                result = Maybe.nullable(value);
                if (result.isPresent()) {
                    return result;
                }
            }
        }
        return result;
    }

    /**
     * Slowest lookup; filters both key and value at the same time,
     * meaning fuller reads (O(n) calls to .get()).
     *
     * Use {@link #firstWhereKeyValue(Filter1, Filter1)} to filter keys before loading values.
     *
     * @param filter
     * @return
     */
    default Maybe<V> firstWhereKeyValue(Filter2<Object, K, V> filter) {
        Maybe<V> result = Maybe.not();
        for (K key : keys()) {
            V value = get(key);
            if (filter.filter2(key, value)) {
                result = Maybe.nullable(value);
                if (result.isPresent()) {
                    return result;
                }
            }
        }
        return result;
    }

    default Maybe<V> firstWhereKeyValue(Filter1<K> keyFilter, Filter1<V> valueFilter) {
        Maybe<V> result = Maybe.not();
        for (K key : keys()) {
            if (keyFilter.filter1(key)) {
                V value = get(key);
                if (valueFilter.filter1(value)) {
                    result = Maybe.nullable(value);
                    if (result.isPresent()) {
                        return result;
                    }
                }
            }
        }
        return result;
    }

}
