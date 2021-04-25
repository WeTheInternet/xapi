package xapi.collect.api;

import xapi.collect.proxy.api.CollectionProxy;
import xapi.fu.*;
import xapi.fu.Filter.Filter1;
import xapi.fu.Filter.Filter2;
import xapi.fu.data.MapLike;
import xapi.fu.itr.SizedIterable;
import xapi.fu.itr.SizedIterator;
import xapi.fu.java.EntryIterable;

public interface ObjectTo <K, V>
extends EntryIterable<K,V>, CollectionProxy<K,V>, HasValues<K,V>
{

  V getOrCompute(K key, In1Out1<K,V> factory);

  default V compute(K key, In1Out1<V,V> factory) {
      return computeBoth(key, factory.ignoresIn1());
  }

  default V computeBoth(K key, In2Out1<K, V,V> factory) {
      final V was = get(key);
      final V is = factory.io(key, was);
      put(key, is);
      return was;
  }

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

    @Override
    default SizedIterable<V> forEachValue() {
        return HasValues.super.forEachValue();
    }

    default MapLike<K, V> asMap() {
      ObjectTo<K, V> values = this;
      return new MapLike<K, V>() {
          @Override
          public SizedIterator<Out2<K, V>> iterator() {
              return forEachEntry().iterator();
          }

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

    default Maybe<V> firstWhereKey(In1Out1<K, Boolean> key) {
        Maybe<V> result = Maybe.not();
        for (K cls : keys()) {
            if (key.io(cls)) {
                final V value = get(cls);
                result = Maybe.nullable(value);
                if (result.isPresent()) {
                    return result;
                }
            }
        }
        return result;
    }

    default Maybe<V> firstWhereValue(In1Out1<V, Boolean> key) {
        Maybe<V> result = Maybe.not();
        for (V value : values()) {
            if (key.io(value)) {
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
     * Use {@link #firstWhereKeyValue(In1Out1, In1Out1)} to filter keys before loading values.
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

    default Maybe<V> firstWhereKeyValue(In1Out1<K, Boolean> keyFilter, In1Out1<V, Boolean> valueFilter) {
        Maybe<V> result = Maybe.not();
        for (K key : keys()) {
            if (keyFilter.io(key)) {
                V value = get(key);
                if (valueFilter.io(value)) {
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
