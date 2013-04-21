package xapi.collect.api;

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

  void removeAll(Iterable<K> items);

  Iterable<K> keys();

  Iterable<V> values();
}
