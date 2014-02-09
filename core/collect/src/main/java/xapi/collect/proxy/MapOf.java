package xapi.collect.proxy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.google.gwt.reflect.client.GwtReflect;

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

  public MapOf(Map<K, V> map, Class<K> keyClass, Class<V> valueClass) {
    this.map = map;
    this.keyClass = keyClass;
    this.valueClass = valueClass;
  }

  @Override
  public ObjectTo<K,V> clone(CollectionOptions options) {
    ObjectTo<K,V> into = X_Collect.newMap(keyClass, valueClass, options);
    for (Entry<K,V> entry : map.entrySet()) {
      // do not give access to our map's entry objects;
      // this method is clone(), which implies copy and not reference sharing
      into.put(entry.getKey(), entry.getValue());
    }
    return into;
  }

  @Override
  public V get(Object key) {
    return map.get(key);
  }

  @Override
  public V remove(Object key) {
    return map.remove(key);
  }

  @Override
  public V[] toArray() {
    V[] values = GwtReflect.newArray(valueClass, map.size());
    map.values().toArray(values);
    return values;
  }

  @Override
  public Collection<V> toCollection(Collection<V> into) {
    if (into == null)
      into = new ArrayList<V>();
    into.addAll(map.values());
    return into;
  }

  @Override
  public Map<K,V> toMap(Map<K,V> into) {
    if (into == null)
      into = new LinkedHashMap<K,V>();
    into.putAll(map);
    return into;
  }

  @Override
  public V put(Entry<K, V> item) {
    return map.put(item.getKey(), item.getValue());
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }

  @Override
  public void clear() {
    map.clear();
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
      public V setValue(V value) {
        return map.put((K)key, value);
      }
    }

    return new EntryProxy();
  }

  @Override
  @SuppressWarnings("unchecked")
  public void setValue(Object key, Object value) {
    map.put((K)key, (V)value);
  }

  @Override
  public boolean containsKey(Object key) {
    return map.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return map.containsValue(value);
  }

  @Override
  public V put(K key, V value) {
    return map.put(key, value);
  }

  @Override
  public void putAll(Map<? extends K,? extends V> m) {
    map.putAll(m);
  }

  @Override
  public Set<K> keySet() {
    return map.keySet();
  }

  @Override
  public Collection<V> values() {
    return map.values();
  }

  @Override
  public Set<java.util.Map.Entry<K,V>> entrySet() {
    return map.entrySet();
  }

  @Override
  public Iterable<java.util.Map.Entry<K,V>> entries() {
    return entrySet();
  }

  @Override
  public void putAll(Iterable<java.util.Map.Entry<K,V>> items) {
    for (Entry<K, V> entry : items) {
      map.put(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public void removeAll(Iterable<K> keys) {
    for (K key : keys) {
      map.remove(key);
    }
  }

  @Override
  public Iterable<K> keys() {
    return keySet();
  }

  @Override
  public Class<K> keyType() {
    return keyClass;
  }

  @Override
  public Class<V> valueType() {
    return valueClass;
  }

  @Override
  public Class<?> componentType() {
    return valueType();
  }

}
