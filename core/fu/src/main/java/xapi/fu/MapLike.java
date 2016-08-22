package xapi.fu;

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
   * A remove operation.  Returns the deleted value, if any.
     */
  V remove(K key);

  MappedIterable<Out2<K, V>> view();

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
