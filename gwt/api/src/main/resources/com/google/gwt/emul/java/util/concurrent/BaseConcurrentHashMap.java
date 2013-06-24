package java.util;

import java.util.*;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * Implementation of Map interface based on a hash table. <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/HashMap.html">[Sun
 * docs]</a>
 * 
 * @param <K>
 *          key type
 * @param <V>
 *          value type
 */
public class BaseConcurrentHashMap<K, V> extends AbstractHashMap<K, V>
    implements Cloneable, Serializable, Map<K, V>, ConcurrentMap<K, V> {

  /**
   * Ensures that RPC will consider type parameter K to be exposed. It will be
   * pruned by dead code elimination.
   */
  @SuppressWarnings("unused")
  private K exposeKey;

  /**
   * Ensures that RPC will consider type parameter V to be exposed. It will be
   * pruned by dead code elimination.
   */
  @SuppressWarnings("unused")
  private V exposeValue;

  public BaseConcurrentHashMap() {
  }

  public BaseConcurrentHashMap(int ignored) {
    super(ignored);
  }

  public BaseConcurrentHashMap(int ignored, float alsoIgnored) {
    super(ignored, alsoIgnored);
  }

  public BaseConcurrentHashMap(Map<? extends K, ? extends V> toBeCopied) {
    super(toBeCopied);
  }

  @Override
  public Object clone() {
    return new BaseConcurrentHashMap<K, V>(this);
  }

  @Override
  protected boolean equals(Object value1, Object value2) {
    return Utility.equalsWithNullCheck(value1, value2);
  }

  @Override
  protected int getHashCode(Object key) {
    // Coerce to int -- our classes all do this, but a user-written class might
    // not.
    return ~~key.hashCode();
  }

  /**
   * If the specified key is not already associated with a value, associate it
   * with the given value. Performs
   * 
   * <pre>
   * if (!map.containsKey(key))
   *   return map.put(key, value);
   * else
   *   return map.get(key);
   * </pre>
   */
  public V putIfAbsent(K key, V value) {
    if (!containsKey(key))
      return put(key, value);
    else
      return get(key);
  }

  /**
   * Removes the entry for a key only if currently mapped to a given value.
   * Performs
   * 
   * <pre>
   * if (map.containsKey(key) &amp;&amp; map.get(key).equals(value)) {
   *   map.remove(key);
   *   return true;
   * } else
   *   return false;
   * </pre>
   */
  public boolean remove(Object key, Object value) {
    if (containsKey(key) && get(key).equals(value)) {
      remove(key);
      return true;
    } else
      return false;
  }

  /**
   * Replaces the entry for a key only if currently mapped to a given value.
   * Performs
   * 
   * <pre>
   * if (map.containsKey(key) &amp;&amp; map.get(key).equals(oldValue)) {
   *   map.put(key, newValue);
   *   return true;
   * } else
   *   return false;
   * </pre>
   * 
   * except that the action is performed atomically.
   */
  public boolean replace(K key, V oldValue, V newValue) {
    if (containsKey(key) && get(key).equals(oldValue)) {
      put(key, newValue);
      return true;
    } else
      return false;
  }

  /**
   * Replaces the entry for a key only if currently mapped to some value. This
   * performs
   * 
   * <pre>
   * if (map.containsKey(key)) {
   *   return map.put(key, value);
   * } else
   *   return null;
   * </pre>
   * 
   * except that the action is performed atomically.
   */
  public V replace(K key, V value) {
    if (containsKey(key)) {
      return put(key, value);
    } else
      return null;
  }
}