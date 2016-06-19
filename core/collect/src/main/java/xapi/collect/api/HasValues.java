package xapi.collect.api;

import xapi.collect.impl.EntryIterable;
import xapi.fu.In2Out1;
import xapi.fu.Out2;

import java.util.Map.Entry;

public interface HasValues<K,V> extends EntryIterable<K,V> {

  // We don't implement typed getters / setters or removers,
  // Because some implementers of this interface are javascript objects,
  // and the interface's erased typed signature MUST match the final method's type.
  // Setting K to String will result in a Ljava/lang/Object; in the interface,
  // and Ljava/lang/String; in the implemented method.


  boolean isEmpty();

  void clear();

  // We can safely erase all the way to java/lang/Object;
  boolean containsKey(Object key);

  boolean containsValue(Object key);

  // We can also safely use generics as they will be erased
  void putAll(Iterable<Entry<K,V>> items);

  void addAll(Iterable<Out2<K,V>> items);

  void removeAll(Iterable<K> items);

  Iterable<K> keys();

  Iterable<V> values();

  default <R> R reduceKeys(In2Out1<K, R, R> reducer, R initial) {
    for (K k : keys()) {
      initial = reducer.io(k, initial);
    }
    return initial;
  }

  default <R> R reduceValues(In2Out1<V, R, R> reducer, R initial) {
    for (V v : values()) {
      initial = reducer.io(v, initial);
    }
    return initial;
  }

  // TODO: enable once Gwt fork is rebased onto stream support
//  default Stream<K> keyStream() {
//    return StreamSupport.stream(keys().spliterator(), false);
//  }
//
//  default Stream<V> valueStream() {
//    return StreamSupport.stream(values().spliterator(), false);
//  }

  Class<K> keyType();

  Class<V> valueType();

}
