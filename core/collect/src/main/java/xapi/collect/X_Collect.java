package xapi.collect;


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

import static xapi.collect.api.CollectionOptions.asImmutableList;
import static xapi.collect.api.CollectionOptions.asImmutableSet;
import static xapi.collect.api.CollectionOptions.asMutable;
import static xapi.collect.api.CollectionOptions.asMutableList;
import static xapi.collect.api.CollectionOptions.asMutableSet;
import static xapi.inject.X_Inject.singleton;

import java.util.Comparator;

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

  public static <V> ClassTo.Many<V> newClassMultiMap(final Class<V> valueCls) {
    return service.newClassMultiMap(valueCls, MUTABLE);
  }

  public static <K, V> ObjectTo.Many<K, V> newMultiMap(final Class<K> keyCls, final Class<V> valueCls) {
    return service.newMultiMap(keyCls, valueCls, MUTABLE);
  }

  public static <T> Fifo<T> newFifo() {
    return service.newFifo();
  }

  public static <X> IntTo.Many<X> newIntMultiMap(final Class<X> component) {
    return new IntToManyList<X>(component);
  }

  /**
   * Creates a new IntTo that is optimized for the current platform.
   * <p>
   * Be careful with this method!  The generic bounds allow you to send any supertype of the class you want the IntTo to contain,
   * however, this is only meant to make it easy to specify types which themselves have generics.
   * <pre>
   * IntTo<Optional<T>> list = X_Collect.newList(Optional.class); // +1, we ignore the &lt;T>
   * IntTo<Integer> = X_Collect.newList(Number.class); // -1, if we call .toArray(), we will get a class cast exception. :-/
   * </pre>
   *
   *
   */
  public static <T, R extends T> IntTo<R> newList(final Class<T> cls) {
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
    return new StringToDeepMap<>(component);
  }

  public static <V> StringDictionary<V> newDictionary() {
    StringDictionary filthyLie = service.newDictionary(Object.class);
    return filthyLie;
  }

  public static <V> StringDictionary<V> newDictionary(Class<V> cls) {
    return service.newDictionary(cls);
  }

  public static <V> StringTo<V> newStringMap(final Class<V> valueCls) {
    return service.newStringMap(valueCls, MUTABLE);
  }

  public static StringTo<Object> newStringMap() {
    return newStringMap(Object.class);
  }

  public static <V> StringTo<V> newStringMapInsertionOrdered(final Class<V> valueCls) {
    return service.newStringMap(valueCls, MUTABLE_INSERTION_ORDERED);
  }

  public static <X> StringTo.Many<X> newStringMultiMap(final Class<X> component) {
    return service.newStringMultiMap(component, MUTABLE);
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
