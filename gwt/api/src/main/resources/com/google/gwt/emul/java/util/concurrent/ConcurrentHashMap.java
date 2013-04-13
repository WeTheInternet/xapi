
package java.util.concurrent;
import java.util.*;
import java.io.Serializable;
import java.util.Map;

/**
 * Implementation of Map interface based on a hash table. <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/HashMap.html">[Sun
 * docs]</a>
 * 
 * @param <K> key type
 * @param <V> value type
 */
public class ConcurrentHashMap<K, V> extends BaseConcurrentHashMap<K, V> implements Cloneable,
    Serializable, Map<K, V> {

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

  public ConcurrentHashMap() {
  }

  public ConcurrentHashMap(int ignored) {
    super(ignored);
  }

  public ConcurrentHashMap(int ignored, float alsoIgnored) {
    super(ignored, alsoIgnored);
  }

  public ConcurrentHashMap(Map<? extends K, ? extends V> toBeCopied) {
    super(toBeCopied);
  }

  @Override
  public Object clone() {
    return new ConcurrentHashMap<K, V>(this);
  }


  @Override
  protected int getHashCode(Object key) {
    // Coerce to int -- our classes all do this, but a user-written class might not.
    return ~~key.hashCode();
  }
}