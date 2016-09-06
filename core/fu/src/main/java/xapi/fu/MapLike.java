package xapi.fu;

import xapi.fu.Filter.Filter1;
import xapi.fu.Filter.Filter2;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/10/16.
 */
public interface MapLike<K, V> {

  /**
   * A put operation.  Returns the previous value, if any.
   */
  V put(K key, V value);

  /**
   * A get operation.  Returns the known value, if any.
     */
  V get(K key);

  /**
   * Check if there is an entry for the given key.
   */
  boolean has(K key);

  /**
   * A remove operation.  Returns the deleted value, if any.
     */
  V remove(K key);

  Iterable<K> keys();

  default MappedIterable<K> mappedKeys() {
    final Iterable<K> itr = keys();
    if (itr instanceof MappedIterable) {
      return (MappedIterable<K>) itr;
    }
    return MappedIterable.mapped(itr);
  }

  default MappedIterable<V> valuesMapped() {
    return MappedIterable.mapIterable(keys(), this::get);
  }

  default MappedIterable<Out2<K, V>> all() {
    return MappedIterable.mapIterable(keys(), k->Out2.out2Immutable(k, get(k)));
  }

  default Maybe<V> removeIfBoth(K key, Filter1<K> filter) {
    if (filter.filter1(key)) {
      return Maybe.immutable(remove(key));
    }
    return Maybe.not();
  }

  default Maybe<V> removeIfKey(K key, Filter1<K> filter) {
    if (filter.filter1(key)) {
      return Maybe.immutable(remove(key));
    }
    return Maybe.not();
  }

  default Maybe<V> removeIfValue(K key, Filter1<V> filter) {
    final V value = get(key);
    if (filter.filter1(value)) {
      return Maybe.immutable(remove(key));
    }
    return Maybe.not();
  }

  default V mapBoth(K key, In2Out1<K, V, V> mapper) {
      final V is = get(key);
      final V newValue = mapper.io(key, is);
      return put(key, newValue);
  }

  default V mapKey(K key, In1Out1<K, V> mapper) {
    final V newValue = mapper.io(key);
    return put(key, newValue);
  }

  default V mapValue(K key, In1Out1<V, V> mapper) {
    final V is = get(key);
    final V newValue = mapper.io(is);
    return put(key, newValue);
  }

  default V filterKeyMapBoth(K key, Filter1<K> filter, In2Out1<K, V, V> mapper) {
    if (filter.filter1(key)) {
      final V is = get(key);
      final V newValue = mapper.io(key, is);
      return put(key, newValue);
    } else {
      return get(key);
    }
  }

  default V filterKeyMapKey(K key, Filter1<K> filter, In1Out1<K, V> mapper) {
    if (filter.filter1(key)) {
      final V newValue = mapper.io(key);
      return put(key, newValue);
    } else {
      return get(key);
    }
  }

  default V filterKeyMapValue(K key, Filter1<K> filter, In1Out1<V, V> mapper) {
    final V is = get(key);
    if (filter.filter1(key)) {
      final V newValue = mapper.io(is);
      return put(key, newValue);
    }
    return is;
  }

  default V filterValueMapBoth(K key, Filter1<V> filter, In2Out1<K, V, V> mapper) {
    final V is = get(key);
    if (filter.filter1(is)) {
      final V newValue = mapper.io(key, is);
      return put(key, newValue);
    } else {
      return get(key);
    }
  }

  default V filterValueMapKey(K key, Filter1<V> filter, In1Out1<K, V> mapper) {
    final V newValue = mapper.io(key);
    if (filter.filter1(newValue)) {
      return put(key, newValue);
    } else {
      return get(key);
    }
  }

  default V filterOldValueMapValue(K key, Filter1<V> filter, In1Out1<V, V> mapper) {
    final V is = get(key);
    if (filter.filter1(is)) {
    final V newValue = mapper.io(is);
      return put(key, newValue);
    }
    return is;
  }

  default V filterNewValueMapValue(K key, Filter1<V> filter, In1Out1<V, V> mapper) {
    final V is = get(key);
    final V newValue = mapper.io(is);
    if (filter.filter1(newValue)) {
      return put(key, newValue);
    }
    return is;
  }

  default MapLike<K, V> ifKeyMapBoth(K key, Filter1<K> filter, In2Out1<K, V, V> mapper) {
    filterKeyMapBoth(key, filter, mapper);
    return this;
  }

  default MapLike<K, V> ifKeyMapKey(K key, Filter1<K> filter, In1Out1<K, V> mapper) {
    filterKeyMapKey(key, filter, mapper);
    return this;
  }

  default MapLike<K, V> ifKeyMapValue(K key, Filter1<K> filter, In1Out1<V, V> mapper) {
    filterKeyMapValue(key, filter, mapper);
    return this;
  }

  default MapLike<K, V> ifValueMapBoth(K key, Filter1<V> filter, In2Out1<K, V, V> mapper) {
    filterValueMapBoth(key, filter, mapper);
    return this;
  }

  default MapLike<K, V> ifValueMapKey(K key, Filter1<V> filter, In1Out1<K, V> mapper) {
    filterValueMapKey(key, filter, mapper);
    return this;
  }

  default MapLike<K, V> ifOldValueMapValue(K key, Filter1<V> filter, In1Out1<V, V> mapper) {
    filterOldValueMapValue(key, filter, mapper);
    return this;
  }

  default MapLike<K, V> ifNewValueMapValue(K key, Filter1<V> filter, In1Out1<V, V> mapper) {
    filterNewValueMapValue(key, filter, mapper);
    return this;
  }

  default MapLike<K, V> ifBothMapBoth(K key, Filter2<Object, K, V> filter, In2Out1<K, V, V> mapper) {
    final V value = get(key);
    if (filter.filter2(key, value)) {
      final V newValue = mapper.io(key, value);
      put(key, newValue);
    }
    return this;
  }

  default Out2<V, V> putAndReturnBoth(K key, V value) {
    return Out2.out2Immutable(put(key, value), value);
  }

  default Out2<V, V> putIfUnchanged(K key, V previousValue, V value) {
    if (previousValue == get(key)) {
      return putAndReturnBoth(key, value);
    } else {
      value = previousValue;
    }
    return Out2.out2Immutable(previousValue, value);
  }

  default V compute(K key, In2Out1<K, V, V> io) {
    V existing = get(key);
    final V computed = io.io(key, existing);
    if (computed != existing) {
      put(key, computed);
    }
    return computed;
  }

  default V computeReturnPrevious(K key, In2Out1<K, V, V> io) {
    V existing = get(key);
    final V computed = io.io(key, existing);
    if (computed != existing) {
      put(key, computed);
    }
    return existing;
  }
}