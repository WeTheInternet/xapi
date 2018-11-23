package xapi.collect.api;

import xapi.fu.In2Out1;
import xapi.fu.Out2;
import xapi.fu.has.HasEmptiness;
import xapi.fu.has.HasSize;
import xapi.fu.itr.SizedIterable;
import xapi.fu.java.EntryIterable;

import java.util.Iterator;
import java.util.Map.Entry;

public interface HasValues<K,V> extends EntryIterable<K,V>, HasSize, HasEmptiness {

  // We don't implement typed getters / setters or removers,
  // Because some implementers of this interface are javascript objects,
  // and the interface's erased typed signature MUST match the final method's type.
  // Setting K to String will result in a Ljava/lang/Object; in the interface,
  // and Ljava/lang/String; in the implemented method.
  // (TODO: force compile time actors to do assignability matching?).

  void clear();

  // We can safely erase all the way to java/lang/Object;
  boolean containsKey(Object key);

  boolean containsValue(Object key);

  // We can also safely use generics as they will be erased
  void putEntries(Iterable<Entry<K,V>> items);

  default void putEntries(HasValues<K,V> items) {
    putEntries(items.entries());
  }

  void addAll(Iterable<Out2<K,V>> items);

  void removeAll(Iterable<K> items);

  default void removeWhileTrue(In2Out1<K, V, Boolean> filter) {
    final Iterator<Entry<K, V>> itr = entries().iterator();
    while (itr.hasNext()) {
      final Entry<K, V> next = itr.next();
      if (Boolean.TRUE.equals(filter.io(next.getKey(), next.getValue()))) {
        itr.remove();
      } else {
        return;
      }
    }
  }

  SizedIterable<K> keys();

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

  default SizedIterable<V> forEachValue() {
    return EntryIterable.super.forEachValue()
        .promisedSize(this::size);
  }

  @Override
  default SizedIterable<Out2<K, V>> forEachItem() {
    return EntryIterable.super.forEachItem()
        .promisedSize(this::size);
  }

  @Override
  default SizedIterable<K> forEachKey() {
    return EntryIterable.super.forEachKey()
        .promisedSize(this::size);
  }
}
