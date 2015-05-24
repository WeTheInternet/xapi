package xapi.collect.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import xapi.annotation.inject.InstanceDefault;
import xapi.collect.api.StringTo;
import xapi.platform.GwtDevPlatform;
import xapi.platform.JrePlatform;
import xapi.util.X_Runtime;

@JrePlatform
@GwtDevPlatform
@InstanceDefault(implFor=StringTo.class)
public class StringToAbstract <V> implements StringTo<V>{

  private static final long serialVersionUID = 7743120861632536635L;
  private final java.util.Map<String,V> map;

  public StringToAbstract() {
    if (isMultithreaded()) {
      map = new ConcurrentHashMap<String,V>();
    } else {
      map = new HashMap<String,V>();
    }
  }
  public StringToAbstract(final Map<String, V> map) {
    this.map = map;
  }

  protected boolean isMultithreaded() {
    return X_Runtime.isMultithreaded();
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
  public boolean containsKey(final Object key) {
    return map.containsKey(key);
  }

  @Override
  public boolean containsValue(final Object key) {
    return map.containsValue(key);
  }

  @Override
  @SuppressWarnings({"unchecked","rawtypes"})
  public void putAll(final Iterable<java.util.Map.Entry<String,V>> items) {
    if (items instanceof java.util.Map) {
      map.putAll((java.util.Map)items);
    } else {
      for (final java.util.Map.Entry<String, V> item : items) {
        map.put(item.getKey(), item.getValue());
      }
    }
  }

  @Override
  public void removeAll(final Iterable<String> items) {
    for (final String item : items) {
      map.remove(item);
    }
  }

  @Override
  public Iterable<String> keys() {
    return map.keySet();
  }

  @Override
  public String[] keyArray() {
    return map.keySet().toArray(new String[0]);
  }

  @Override
  public Iterable<V> values() {
    return map.values();
  }

  @Override
  public Iterable<java.util.Map.Entry<String,V>> entries() {
    return map.entrySet();
  }

  @Override
  public V get(final String key) {
    return map.get(key);
  }

  @Override
  public V put(final String key, final V value) {
    if (value == null) {
      return map.remove(key);
    } else {
      return map.put(key, value);
    }
  }

  @Override
  public V remove(final String key) {
    return map.remove(key);
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public String toString() {
    return map.toString();
  }

  protected Collection<V> valueSet() {
    return map.values();
  }

}
