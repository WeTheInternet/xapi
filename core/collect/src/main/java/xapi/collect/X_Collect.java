package xapi.collect;


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
import xapi.collect.impl.ArrayIterator;
import xapi.collect.impl.HashComparator;
import xapi.collect.impl.SingletonIterator;
import xapi.collect.service.CollectionService;
import xapi.inject.X_Inject;
import xapi.util.api.ReceivesValue;
import static xapi.collect.api.CollectionOptions.asImmutableList;
import static xapi.collect.api.CollectionOptions.asImmutableSet;
import static xapi.collect.api.CollectionOptions.asMutable;
import static xapi.collect.api.CollectionOptions.asMutableList;
import static xapi.collect.api.CollectionOptions.asMutableSet;
import static xapi.inject.X_Inject.singleton;

public class X_Collect {

  private X_Collect() {}
  
  public static final CollectionService service = singleton(CollectionService.class);

  public static final CollectionOptions IMMUTABLE = asMutable(false).build();
  public static final CollectionOptions IMMUTABLE_LIST = asImmutableList().build();
  public static final CollectionOptions IMMUTABLE_SET = asImmutableSet().build();
  public static final CollectionOptions MUTABLE = asMutable(true).build();
  public static final CollectionOptions MUTABLE_LIST = asMutableList().build();
  public static final CollectionOptions MUTABLE_SET = asMutableSet().build();

  public static <V> IntTo<V> newList(Class<V> cls) {
    return service.newList(cls, MUTABLE_LIST);
  }

  public static <V> IntTo<V> asList(@SuppressWarnings("unchecked") V ... elements) {
    @SuppressWarnings("unchecked")
    IntTo<V> list = (IntTo<V>)newList(elements.getClass().getComponentType());
    for (V item : elements)
      list.push(item);
    return list;
  }

  public static <K,V> ObjectTo<K,V> newMap(Class<K> keyCls, Class<V> valueCls) {
    return service.newMap(keyCls, valueCls, MUTABLE);
  }
  public static <K,V> ObjectTo<K,V> newMap(Class<K> keyCls, Class<V> valueCls, CollectionOptions opts) {
    return service.newMap(keyCls, valueCls, opts);
  }

  public static <V> ClassTo<V> newClassMap(Class<V> valueCls) {
    return service.newClassMap(valueCls, MUTABLE);
  }

  public static <V> StringTo<V> newStringMap(Class<V> valueCls) {
    return service.newStringMap(valueCls, MUTABLE);
  }

  public static StringDictionary<String> newStringDictionary() {
    return service.newDictionary();
  }

  public static <V> IntTo<V> newSet(Class<V> cls) {
    return service.newList(cls, MUTABLE_SET);
  }

  public static <V> IntTo<V> asSet(@SuppressWarnings("unchecked") V ... elements) {
    @SuppressWarnings("unchecked")
    IntTo<V> list = (IntTo<V>)newSet(elements.getClass().getComponentType());
    for (V item : elements)
      list.push(item);
    return list;
  }

  public static <T> Comparator<T> getComparator(CollectionOptions opts) {
    return new HashComparator<T>();
  }

  @SuppressWarnings("unchecked")
  public static <T> Fifo<T> newFifo() {
    return service.newFifo();
  }

  public static <K, V> void copyInto(HasValues<K, V> from, HasValues<K, V> into) {
    into.putAll(from.entries());
  }

  public static <K, V> void copyDictionary(final Dictionary<K, V> from,final Dictionary<K,V> into) {
    from.forKeys(new ReceivesValue<K>() {
      @Override
      public void set(K key) {
        into.setValue(key, from.getValue(key));
      }
    });
  }

  public static <T, S extends T> Iterable<T> iterable(S item) {
    return new SingletonIterator<T>(item);
  }
  public static <T, S extends T> Iterable<T> iterable(S ... items) {
    return new ArrayIterator<T>(items);
  }

}
