package xapi.collect.service;

import xapi.collect.api.ClassTo;
import xapi.collect.api.CollectionOptions;
import xapi.collect.api.Fifo;
import xapi.collect.api.IntTo;
import xapi.collect.api.ObjectTo;
import xapi.collect.api.StringDictionary;
import xapi.collect.api.StringTo;
import xapi.collect.proxy.CollectionProxy;

public interface CollectionService {

  <E, Generic extends E> IntTo<E> newList(Class<Generic> cls, CollectionOptions opts);

  <E, Generic extends E> IntTo<E> newSet(Class<Generic> cls, CollectionOptions opts);

  <K, V, Key extends K, Value extends V> ObjectTo<K, V> newMap(Class<Key> key, Class<Value> cls, CollectionOptions opts);

  <K, V> ObjectTo.Many<K, V> newMultiMap(Class<K> key, Class<V> cls, CollectionOptions opts);

  <V, Generic extends V> ClassTo<V> newClassMap(Class<Generic> cls, CollectionOptions opts);

  <V, Generic extends V> ClassTo.Many<V> newClassMultiMap(Class<Generic> cls, CollectionOptions opts);

  <V, Generic extends V> StringTo<V> newStringMap(Class<Generic> cls, CollectionOptions opts);

  <V, Generic extends V> StringTo.Many<V> newStringMultiMap(Class<Generic> cls, CollectionOptions opts);

  <V> StringDictionary<V> newDictionary(Class<V> cls);

  <V> Fifo<V> newFifo();

  <K, V, Key extends K, Value extends V> CollectionProxy<K,V> newProxy(Class<Key> keyCls, Class<Value> valueCls, CollectionOptions opts);
}
