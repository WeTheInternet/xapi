package xapi.collect.service;

import xapi.collect.api.ClassTo;
import xapi.collect.api.CollectionOptions;
import xapi.collect.api.Fifo;
import xapi.collect.api.IntTo;
import xapi.collect.api.ObjectTo;
import xapi.collect.api.StringDictionary;
import xapi.collect.api.StringTo;

public interface CollectionService {

  <E, Generic extends E> IntTo<E> newList(Class<Generic> cls, CollectionOptions opts);

  <V> IntTo<V> newSet(Class<V> cls, CollectionOptions opts);

  <K, V> ObjectTo<K, V> newMap(Class<K> key, Class<V> cls, CollectionOptions opts);

  <K, V> ObjectTo.Many<K, V> newMultiMap(Class<K> key, Class<V> cls, CollectionOptions opts);

  <V, C extends Class<? extends V>> ClassTo<V> newClassMap(C cls, CollectionOptions opts);

  <V> ClassTo.Many<V> newClassMultiMap(Class<V> cls, CollectionOptions opts);

  <V> StringTo<V> newStringMap(Class<? extends V> cls, CollectionOptions opts);

  <V> StringTo.Many<V> newStringMultiMap(Class<V> cls, CollectionOptions opts);

  <V> StringDictionary<V> newDictionary(Class<V> cls);

  <V> Fifo<V> newFifo();

}
