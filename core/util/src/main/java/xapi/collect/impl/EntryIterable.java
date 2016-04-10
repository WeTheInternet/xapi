package xapi.collect.impl;

import xapi.fu.In2;

import java.util.Map.Entry;

public interface EntryIterable <K, V> {

  Iterable<Entry<K,V>> entries();

  default EntryIterable <K, V> iterate(In2<K, V> callback) {
    entries().forEach(
        // Need to be explicit about the generics for some reason :-/
        callback.<Entry<K, V>>adapt(Entry::getKey, Entry::getValue)
            .toConsumer()
    );
    return this;

  }

  default void forBoth(In2<K, V> callback) {
    entries().forEach(callback.mapAdapter());
  }

}
