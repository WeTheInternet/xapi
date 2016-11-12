package xapi.collect.proxy;

import xapi.collect.api.CollectionOptions;
import xapi.collect.api.ObjectTo;
import xapi.collect.impl.SimpleStack;
import xapi.fu.In1;
import xapi.fu.In2;
import xapi.fu.In2.In2Unsafe;
import xapi.fu.In2Out1;
import xapi.fu.Out2;
import xapi.fu.iterate.ArrayIterable;

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

   default boolean readWhileTrue(In1<Out2<K, V>> callback, In2Out1<K, V, Boolean> filter) {
     return readWhileTrue((k, v)->{
       if (filter.io(k, v)) {
         Out2<K, V> o = Out2.out2Immutable(k, v);
         callback.in(o);
         return true;
       }
       return false;
     });
   }

   default void forEachValue(In1<V> callback) {
     // purposely create a copy. This will avoid comodification exception
     for (V v : toArray()) {
       callback.in(v);
     }
   }

   default void forEachPairUnsafe(In2Unsafe<K, V> callback) {
       forEachPair(callback);
   }
   default void forEachPair(In2<K, V> callback) {
     // purposely create a copy. This will avoid comodification exception
     readWhileTrue(callback.supply1(true));
   }

   default Iterable<Out2<K, V>> forEachEntry() {
     SimpleStack<Out2<K, V>> stack = new SimpleStack<>();
     readWhileTrue(stack::add, (k, v)->true);
     return stack;
   }

  default Iterable<V> iterateValues() {
    final V[] arr = toArray();
    return new ArrayIterable<>(arr);
  }

  default String toSource() {
      StringBuilder b = new StringBuilder("{");
      String sep = "";
      for (Out2<K, V> entry : forEachEntry()) {
          b.append(sep);
          sep = ", ";
          b.append(toString(entry.out1(), entry.out2()));
      }
      b.append("}");
      return b.toString();
  }

  default String toString(K key, V value) {
      return key + "=" + value ;
  }
}
