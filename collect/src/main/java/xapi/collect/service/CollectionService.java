package xapi.collect.service;

import xapi.collect.api.ClassTo;
import xapi.collect.api.CollectionOptions;
import xapi.collect.fifo.Fifo;
import xapi.collect.api.IntTo;
import xapi.collect.api.ObjectTo;
import xapi.collect.api.StringDictionary;
import xapi.collect.api.StringTo;
import xapi.collect.proxy.api.CollectionProxy;

public interface CollectionService {

  // This weird "double type parameter crap" <E, Generic extends E> is the necessary hoops to jump through
  // in order to get valid, correct type inference when doing IntTo<MyType> list = X_Collect.newList(MyType.class);
  // It is still ugly and causes warnings in the ide, but it actually is the most typesafe and correct option.
  // It will be a compile error if you get the class wrong, and only a warning (that you can safely @Ignore).
  // We (try to) use class literal lookups at compile time to inline calls that can safely bypass us altogether.

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
