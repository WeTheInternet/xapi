package xapi.collect.impl;

import xapi.annotation.inject.InstanceDefault;
import xapi.collect.api.StringTo;
import xapi.fu.Out2;
import xapi.fu.itr.MappedIterable;
import xapi.fu.itr.SizedIterable;
import xapi.fu.itr.SizedIterator;
import xapi.platform.GwtDevPlatform;
import xapi.platform.JrePlatform;
import xapi.util.X_Runtime;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@JrePlatform
@GwtDevPlatform
@InstanceDefault(implFor=StringTo.class)
public class StringToAbstract <V> implements StringTo<V>{

  private static final long serialVersionUID = 7743120861632536635L;
  private final java.util.Map<String,V> map;
  private final Class<V> valueType;
  private final AtomicBoolean recurseProtect = new AtomicBoolean(false);

  public StringToAbstract(Class<V> valueType) {
    this.valueType = valueType;
    if (isMultithreaded()) {
      map = new ConcurrentHashMap<>();
    } else {
      map = new HashMap<>();
    }
  }

  public <Generic extends V> StringToAbstract(Class<Generic> valueType, final Map<String, V> map) {
    this.valueType = Class.class.cast(valueType);
    this.map = map;
  }

  protected boolean isMultithreaded() {
    return X_Runtime.isMultithreaded();
  }

  @Override
  public SizedIterator<Out2<String, V>> iterator() {
    final MappedIterable<Entry<String, V>> entries = MappedIterable.mapped(entries());
    return SizedIterator.of(this::size, entries.map(Out2::fromEntry).iterator());
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
  public final boolean containsKey(final Object key) {
    return has((String)key);
  }

  public boolean has(String key) {
    return map.containsKey(key);
  }
  @Override
  public boolean containsValue(final Object key) {
    return map.containsValue(key);
  }

  @Override
  @SuppressWarnings({"unchecked","rawtypes"})
  public void putEntries(final Iterable<java.util.Map.Entry<String,V>> items) {
    if (items instanceof java.util.Map) {
      map.putAll((java.util.Map)items);
    } else {
      for (final java.util.Map.Entry<String, V> item : items) {
        map.put(item.getKey(), item.getValue());
      }
    }
  }

  @Override
  public void addAll(Iterable<Out2<String, V>> items) {
    for (final Out2<String, V> item : items) {
      map.put(item.out1(), item.out2());
    }
  }

  @Override
  public void removeAll(final Iterable<String> items) {
    for (final String item : items) {
      map.remove(item);
    }
  }

  @Override
  public SizedIterable<String> keys() {
    return SizedIterable.of(this::size, map.keySet());
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
  public Class<String> keyType() {
    return String.class;
  }

  @Override
  public Class<V> valueType() {
    return valueType;
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
    try {
      if (recurseProtect.get()) {
        return "<INFINITE RECURSION DETECTED; BAILING...>";
      }
      recurseProtect.set(true);
      return forEachItem().join(o->o.join(" = "), "\n");
    } finally {
        recurseProtect.set(false);
    }
  }

  protected Collection<V> valueSet() {
    return map.values();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    final StringToAbstract<?> that = (StringToAbstract<?>) o;

    if (!map.equals(that.map))
      return false;
    return valueType != null ? valueType.equals(that.valueType) : that.valueType == null;
  }

  @Override
  public int hashCode() {
    int result = map.hashCode();
    result = 31 * result + (valueType != null ? valueType.hashCode() : 0);
    return result;
  }
}
