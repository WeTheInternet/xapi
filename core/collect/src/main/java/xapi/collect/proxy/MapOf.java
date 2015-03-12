package xapi.collect.proxy;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import xapi.collect.X_Collect;
import xapi.collect.api.CollectionOptions;
import xapi.collect.api.HasValues;
import xapi.collect.api.ObjectTo;

public class MapOf <K, V>
implements CollectionProxy<K,V>, Map<K,V>, HasValues<K,V>, ObjectTo<K,V>
{

  private final Class<K> keyClass;
  private final Map<K,V> map;
  private final Class<V> valueClass;

  public MapOf(final Map<K, V> map, final Class<K> keyClass, final Class<V> valueClass) {
    this.map = map;
    this.keyClass = keyClass;
    this.valueClass = valueClass;
  }

  @Override
  public void clear() {
    map.clear();
  }

  @Override
  public ObjectTo<K,V> clone(final CollectionOptions options) {
    final ObjectTo<K,V> into = X_Collect.newMap(keyClass, valueClass, options);
    for (final Entry<K,V> entry : map.entrySet()) {
      // do not give access to our map's entry objects;
      // this method is clone(), which implies copy and not reference sharing
      into.put(entry.getKey(), entry.getValue());
    }
    return into;
  }

  @Override
  public Class<?> componentType() {
    return valueType();
  }

  @Override
  public boolean containsKey(final Object key) {
    return map.containsKey(key);
  }

  @Override
  public boolean containsValue(final Object value) {
    return map.containsValue(value);
  }

  @Override
  public Iterable<java.util.Map.Entry<K,V>> entries() {
    return entrySet();
  }

  @Override
  @SuppressWarnings("unchecked")
  public Entry<K,V> entryFor(final Object key) {
    // Encourage inlining; EntryProxy won't get loaded if we only
    // use CollectionProxy based maps.
    if (map instanceof CollectionProxy) {
      return ((CollectionProxy<K,V>)map).entryFor(key);
    }
    // Rather than iterate to get the map's actual Entry,
    // We'll just return a proxy object that fulfills the same duty.
    class EntryProxy implements Entry<K, V> {

      @Override
      public K getKey() {
        return (K)key;
      }

      @Override
      public V getValue() {
        return map.get(key);
      }

      @Override
      public V setValue(final V value) {
        return map.put((K)key, value);
      }
    }

    return new EntryProxy();
  }

  @Override
  public Set<java.util.Map.Entry<K,V>> entrySet() {
    return map.entrySet();
  }

  @Override
  public V get(final Object key) {
    return map.get(key);
  }

  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }

  @Override
  public Iterable<K> keys() {
    return keySet();
  }

  @Override
  public Set<K> keySet() {
    return map.keySet();
  }

  @Override
  public Class<K> keyType() {
    return keyClass;
  }

  @Override
  public V put(final Entry<K, V> item) {
    return map.put(item.getKey(), item.getValue());
  }

  @Override
  public V put(final K key, final V value) {
    return map.put(key, value);
  }

  @Override
  public void putAll(final Iterable<java.util.Map.Entry<K,V>> items) {
    for (final Entry<K, V> entry : items) {
      map.put(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public void putAll(final Map<? extends K,? extends V> m) {
    map.putAll(m);
  }

  @Override
  public V remove(final Object key) {
    return map.remove(key);
  }

  @Override
  public void removeAll(final Iterable<K> keys) {
    for (final K key : keys) {
      map.remove(key);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public void setValue(final Object key, final Object value) {
    map.put((K)key, (V)value);
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public V[] toArray() {
    final V[] values = (V[]) Array.newInstance(valueClass, map.size());
    map.values().toArray(values);
    return values;
  }

  @Override
  public Collection<V> toCollection(Collection<V> into) {
    if (into == null) {
      into = new ArrayList<V>();
    }
    into.addAll(map.values());
    return into;
  }

  @Override
  public Map<K,V> toMap(Map<K,V> into) {
    if (into == null) {
      into = new LinkedHashMap<K,V>();
    }
    into.putAll(map);
    return into;
  }

  @Override
  public Collection<V> values() {
    return map.values();
  }

  @Override
  public Class<V> valueType() {
    return valueClass;
  }

}
