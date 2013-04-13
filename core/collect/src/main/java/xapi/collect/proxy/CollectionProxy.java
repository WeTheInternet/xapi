package xapi.collect.proxy;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import xapi.collect.api.CollectionOptions;
import xapi.collect.api.ObjectTo;

public interface CollectionProxy <K, V>
{

  ObjectTo<K, V> clone(CollectionOptions options);

  V put(Entry<K,V> item);

  Entry<K,V> entryFor(Object key);

  V get(Object key);

  void setValue(Object key, Object value);

  V remove(Object key);

  int size();

  V[] toArray();

  Collection<V> toCollection(Collection<V> into);

  Map<K, V> toMap(Map<K, V> into);

  boolean isEmpty();

  void clear();

}
