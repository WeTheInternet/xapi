package xapi.collect.proxy;

import xapi.collect.api.CollectionOptions;
import xapi.collect.api.ObjectTo;
import xapi.collect.impl.ArrayIterable;
import xapi.fu.In1;
import xapi.fu.In2;
import xapi.fu.In2Out1;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

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

  default boolean isNotEmpty() {
    return !isEmpty();
  }

  void clear();

  Class<K> keyType();

  Class<V> valueType();

   boolean readWhileTrue(In2Out1<K, V, Boolean> callback);

   default void forEachValue(In1<V> callback) {
     // purposely create a copy. This will avoid comodification exception
     for (V v : toArray()) {
       callback.in(v);
     }
   }

   default void forEachPair(In2<K, V> callback) {
     // purposely create a copy. This will avoid comodification exception
     readWhileTrue(callback.supply1(true));
   }

  default Iterable<V> iterateValues() {
    final V[] arr = toArray();
    return new ArrayIterable<>(arr);
  };
}
