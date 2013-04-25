package xapi.collect.impl;

import java.util.Map.Entry;

public interface EntryIterable <K, V> {

  Iterable<Entry<K,V>> entries();

}
