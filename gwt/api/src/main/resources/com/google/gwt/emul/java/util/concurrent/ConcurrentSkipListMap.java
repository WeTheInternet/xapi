package java.util.concurrent;

import java.util.*;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * Very basic emulation for SkipListMap; it's just a TreeMap, to implement
 * all of the {@link NavigableMap} functionality.
 * 
 * TODO: implement ConcurrentMap extensions properly.
 * 
 * @param <K> key type
 * @param <V> value type
 */
public class ConcurrentSkipListMap <K,V> extends TreeMap<K,V> implements Cloneable, Serializable {

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

  public ConcurrentSkipListMap() {
    super();
  }
  
  public ConcurrentSkipListMap(Comparator<? super K> comparator) {
    super(comparator);
  }

  /**
   * Constructs a new map containing the same mappings as the given map, sorted
   * according to the {@linkplain Comparable natural ordering} of the keys.
   * 
   * @param m the map whose mappings are to be placed in this map
   * @throws ClassCastException if the keys in <tt>m</tt> are not
   * {@link Comparable}, or are not mutually comparable
   * @throws NullPointerException if the specified map or any of its keys or
   * values are null
   */
  public ConcurrentSkipListMap(Map<? extends K,? extends V> m) {
    super(m);
  }

  /**
   * Constructs a new map containing the same mappings and using the same
   * ordering as the specified sorted map.
   * 
   * @param m the sorted map whose mappings are to be placed in this map, and
   * whose comparator is to be used to sort this map
   * @throws NullPointerException if the specified sorted map or any of its keys
   * or values are null
   */
  public ConcurrentSkipListMap(SortedMap<K,? extends V> m) {
    super(m);
  }

  public Object clone() {
    return new ConcurrentSkipListMap<K,V>(this);
  }

}
