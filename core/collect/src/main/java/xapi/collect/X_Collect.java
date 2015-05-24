package xapi.collect;


import static xapi.collect.api.CollectionOptions.asImmutableList;
import static xapi.collect.api.CollectionOptions.asImmutableSet;
import static xapi.collect.api.CollectionOptions.asMutable;
import static xapi.collect.api.CollectionOptions.asMutableList;
import static xapi.collect.api.CollectionOptions.asMutableSet;
import static xapi.inject.X_Inject.singleton;

import java.util.Comparator;

import xapi.collect.api.ClassTo;
import xapi.collect.api.CollectionOptions;
import xapi.collect.api.Dictionary;
import xapi.collect.api.Fifo;
import xapi.collect.api.HasValues;
import xapi.collect.api.IntTo;
import xapi.collect.api.ObjectTo;
import xapi.collect.api.StringDictionary;
import xapi.collect.api.StringTo;
import xapi.collect.impl.ArrayIterable;
import xapi.collect.impl.HashComparator;
import xapi.collect.impl.IntToManyList;
import xapi.collect.impl.SingletonIterator;
import xapi.collect.impl.StringToDeepMap;
import xapi.collect.impl.StringToManyList;
import xapi.collect.service.CollectionService;
import xapi.util.api.ReceivesValue;

public class X_Collect {

  public static <V> IntTo<V> asList(@SuppressWarnings("unchecked")
  final V ... elements) {
    @SuppressWarnings("unchecked")
    final
    Class<V> cls = Class.class.cast(elements.getClass().getComponentType());
    final IntTo<V> list = service.newList(cls, MUTABLE_LIST);
    for (final V item : elements) {
      list.push(item);
    }
    return list;
  }

  public static <V> IntTo<V> asSet(@SuppressWarnings("unchecked")
  final V ... elements) {
    @SuppressWarnings("unchecked")
    final
    IntTo<V> list = (IntTo<V>)newSet(elements.getClass().getComponentType());
    for (final V item : elements) {
      list.push(item);
    }
    return list;
  }

  public static <K, V> void copyDictionary(final Dictionary<K, V> from,final Dictionary<K,V> into) {
    from.forKeys(new ReceivesValue<K>() {
      @Override
      public void set(final K key) {
        into.setValue(key, from.getValue(key));
      }
    });
  }
  public static <K, V> void copyInto(final HasValues<K, V> from, final HasValues<K, V> into) {
    into.putAll(from.entries());
  }
  public static <T> Comparator<T> getComparator(final CollectionOptions opts) {
    return new HashComparator<T>();
  }
  public static <T, S extends T> Iterable<T> iterable(final S item) {
    return new SingletonIterator<T>(item);
  }
  public static <T, S extends T> Iterable<T> iterable(@SuppressWarnings("unchecked")
  final S ... items) {
    return new ArrayIterable<T>(items);
  }
  public static <V> ClassTo<V> newClassMap(final Class<V> valueCls) {
    return service.newClassMap(valueCls, MUTABLE);
  }
  public static <T> Fifo<T> newFifo() {
    return service.newFifo();
  }

  public static <X> IntTo.Many<X> newIntMultiMap(final Class<X> component) {
    return new IntToManyList<X>(component);
  }

  public static <V> IntTo<V> newList(final Class<? extends V> cls) {
    return service.newList(cls, MUTABLE_LIST);
  }

  public static <K,V> ObjectTo<K,V> newMap(final Class<K> keyCls, final Class<V> valueCls) {
    return service.newMap(keyCls, valueCls, MUTABLE);
  }
  public static <K,V> ObjectTo<K,V> newMap(final Class<K> keyCls, final Class<V> valueCls, final CollectionOptions opts) {
    return service.newMap(keyCls, valueCls, opts);
  }

  public static <V> IntTo<V> newSet(final Class<V> cls) {
    return service.newList(cls, MUTABLE_SET);
  }

  public static <X> StringTo<StringTo<X>> newStringDeepMap(final Class<? extends X> component) {
    return new StringToDeepMap<X>(component);
  }

  public static <V> StringDictionary<V> newDictionary() {
    return service.newDictionary();
  }

  public static <V> StringTo<V> newStringMap(final Class<V> valueCls) {
    return service.newStringMap(valueCls, MUTABLE);
  }

  public static <V> StringTo<V> newStringMapInsertionOrdered(final Class<V> valueCls) {
    return service.newStringMap(valueCls, MUTABLE_INSERTION_ORDERED);
  }

  public static <X> StringTo.Many<X> newStringMultiMap(final Class<X> component) {
    return new StringToManyList<X>(component);
  }

  public static <X> StringTo.Many<X> newStringMultiMap(final Class<X> component, final java.util.Map<String, IntTo<X>> map) {
    return new StringToManyList<X>(component, map);
  }

  public static final CollectionService service = singleton(CollectionService.class);

  public static final CollectionOptions IMMUTABLE = asMutable(false).build();

  public static final CollectionOptions IMMUTABLE_LIST = asImmutableList().build();

  public static final CollectionOptions IMMUTABLE_SET = asImmutableSet().build();
  public static final CollectionOptions MUTABLE = asMutable(true).build();

  public static final CollectionOptions MUTABLE_INSERTION_ORDERED = asMutable(true).insertionOrdered(true).build();

  public static final CollectionOptions MUTABLE_LIST = asMutableList().build();

  public static final CollectionOptions MUTABLE_SET = asMutableSet().build();

  private X_Collect() {}

}
