package xapi.collect.api;

import xapi.fu.Out2;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/10/16.
 */
public interface TypedMap <K, V> {

  /**
   * A put operation.  Returns the previous value, if any
   */
  V put(K key, V value);

  V get(K key);

  V remove(K key);

    default Out2<V, V> putAndReturnBoth(K key, V value) {
    return Out2.out2(value, put(key, value));
  }

  default Out2<V, V> putIfUnchanged(K key, V previousValue, V value) {
    if (previousValue == get(key)) {
      return putAndReturnBoth(key, value);
    } else {
      value = previousValue;
    }
    return Out2.out2(previousValue, value);
  }

}
