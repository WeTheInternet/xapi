package xapi.collect;

import xapi.annotation.gc.NotReusable;
import xapi.collect.api.ClassTo;
import xapi.collect.api.CollectionOptions;
import xapi.collect.api.Dictionary;
import xapi.collect.api.Fifo;
import xapi.collect.api.HasValues;
import xapi.collect.api.IntTo;
import xapi.collect.api.ObjectTo;
import xapi.collect.api.StringDictionary;
import xapi.collect.api.StringTo;
import xapi.collect.impl.*;
import xapi.collect.proxy.CollectionProxy;
import xapi.collect.service.CollectionService;
import xapi.fu.In2Out1;
import xapi.util.api.ReceivesValue;

import static xapi.collect.api.CollectionOptions.asImmutableList;
import static xapi.collect.api.CollectionOptions.asImmutableSet;
import static xapi.collect.api.CollectionOptions.asMutable;
import static xapi.collect.api.CollectionOptions.asMutableList;
import static xapi.collect.api.CollectionOptions.asMutableSet;
import static xapi.inject.X_Inject.singleton;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

/**
 A wrapper / helper class around {@link CollectionService}.

 We use {@link xapi.inject.X_Inject#singleton} to give us a platform-specific implementation of our underlying collections class.

 Right now, GWT and JVMs get two different versions,
 but any platform you want can and should feel free to inject the collections types of your choosing.

 At this time, there are a number of {@link CollectionOptions} that are NOT supported correctly / at all.
 Basic lists, maps and multimaps work, but all permutations of the settings are not applied correctly.

 If you write an implementation which does handle these classes correctly,
 feel free to submit them as a pull request to git@github.com:JamesXNelson/xapi.git
 or email James@WeTheInter.net with a link so your module can be referred to in (public) documentation.


 A note on the generics structures used in this class:

 Rather than rely on `Class<? super T>`,
 we instead use two generics to represent a type:
 `<Type, Generic extends Type> Type method(Class<Generic> cls);`

 The second generic gives a place for the compiler
 to put the subtypes of the generic Type.

 Plain `<Type>` means the raw type, so
 `Generic extends Type` allows the compiler to infer:
 `List<String> list = method(List.class)`
 Where `Type == List.class` and `Generic == List<String>.class`

 Without the two generics, it is impossible to safely
 infer `SomeType<String>` from `String.class`.
 *
 */
public class X_Collect {

  @SuppressWarnings("all")
  public static <V> IntTo<V> asList(
  final V ... elements) {
    if (elements == null) {
      throw new NullPointerException();
    }
    final Class<V> cls = Class.class.cast(elements.getClass().getComponentType());
    final IntTo<V> list = service.newList(cls, MUTABLE_LIST);
    if (elements.length == 1 && cls.isArray()) {
      // Do not create an IntTo<V[]> by accident!  Bad heap pollution! BAD!

    }
    for (final V item : elements) {
      list.push(item);
    }
    return list;
  }

  public static <V> IntTo<V> asSet(@SuppressWarnings("unchecked")
  final V ... elements) {
    @SuppressWarnings("unchecked")
    final
    Class<V> cls = Class.class.cast(elements.getClass().getComponentType());
    IntTo<V> list = newSet(cls);
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
    return new SingletonIterator<>(item);
  }
  public static <T, S extends T> Iterable<T> arrayIterable(@SuppressWarnings("all") final S ... items) {
    return items == null ? EmptyIterable.EMPTY : new ArrayIterable<>(items);
  }
  @SuppressWarnings({"all"})
  public static <V> ClassTo<V> newClassMap() {
    ClassTo raw = service.newClassMap(Object.class, MUTABLE);
    return raw;
  }
  public static <Type, Generic extends Type> ClassTo<Type> newClassMap(final Class<Generic> valueCls) {
    return service.newClassMap(valueCls, MUTABLE);
  }

  public static <Type, Generic extends Type> ClassTo.Many<Type> newClassMultiMap(final Class<Generic> valueCls) {
    return service.newClassMultiMap(valueCls, MUTABLE);
  }

  public static <Type, Generic extends Type> ClassTo.Many<Type> newClassMultiMap(final Class<Generic> valueCls, CollectionOptions opts) {
    return service.newClassMultiMap(valueCls, opts);
  }

  public static <K, V> ObjectTo.Many<K, V> newMultiMap(final Class<K> keyCls, final Class<V> valueCls) {
    return newMultiMap(keyCls, valueCls, MUTABLE);
  }

  public static <K, V> ObjectTo.Many<K, V> newMultiMap(final Class<K> keyCls, final Class<V> valueCls, CollectionOptions opts) {
    return service.newMultiMap(keyCls, valueCls, opts);
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
  public static <Type, Generic extends Type> IntTo<Type> newList(final Class<Generic> cls) {
    return service.newList(cls, MUTABLE_LIST);
  }

  public static <Type, Generic extends Type> IntTo<Type> newList(final Class<Generic> cls, CollectionOptions opts) {
    return service.newList(cls, CollectionOptions.from(opts).insertionOrdered(true).build());
  }

  public static <Type, Generic extends Type> IntTo<Type> newSet(final Class<Generic> cls, Comparator<Type> cmp) {
    return service.newSet(cls, cmp, CollectionOptions.asInsertionOrdered().build());
  }

  public static <Type, Generic extends Type> IntTo<Type> newSet(final Class<Generic> cls, CollectionOptions opts, Comparator<Type> cmp) {
    return service.newSet(cls, cmp, CollectionOptions.from(opts).insertionOrdered(true).build());
  }

  public static <K,V, Key extends K, Value extends V> ObjectTo<Key,Value> newMap(final Class<Key> keyCls, final Class<Value> valueCls) {
    return service.newMap(keyCls, valueCls, MUTABLE);
  }
  public static <K,V> ObjectTo<K,V> newMap(final Class<K> keyCls, final Class<V> valueCls, final CollectionOptions opts) {
    return service.newMap(keyCls, valueCls, opts);
  }

  public static <Type, Generic extends Type> IntTo<Type> newSet(final Class<Generic> cls) {
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

  public static <V, Generic extends V> StringTo<V> newStringMap(final Class<Generic> valueCls) {
    return service.newStringMap(valueCls, MUTABLE);
  }

  public static StringTo<Object> newStringMap() {
    return newStringMap(Object.class);
  }

  public static <V> StringTo<V> newStringMapInsertionOrdered(final Class<V> valueCls) {
    return service.newStringMap(valueCls, MUTABLE_INSERTION_ORDERED);
  }

  public static <X> StringTo.Many<X> newStringMultiMap(final Class<X> component) {
    return service.newStringMultiMap(component, MUTABLE_INSERTION_ORDERED);
  }

  public static <X> StringTo.Many<X> newStringMultiMap(final Class<X> component, CollectionOptions opts) {
    return service.newStringMultiMap(component, opts);
  }

  public static <X> StringTo.Many<X> newStringMultiMap(final Class<X> component, final java.util.Map<String, IntTo<X>> map) {
    return new StringToManyList<X>(component, map);
  }

  private static final CollectionService service = singleton(CollectionService.class);

  public static CollectionService collections() {
    return service;
  }

  public static final CollectionOptions IMMUTABLE = asMutable(false).build();

  public static final CollectionOptions IMMUTABLE_LIST = asImmutableList().build();

  public static final CollectionOptions IMMUTABLE_SET = asImmutableSet().build();

  public static final CollectionOptions MUTABLE = asMutable(true).build();

  public static final CollectionOptions MUTABLE_INSERTION_ORDERED = asMutable(true).insertionOrdered(true).build();

  public static final CollectionOptions MUTABLE_LIST = asMutableList().build();

  public static final CollectionOptions MUTABLE_SET = asMutableSet().build();

  private X_Collect() {}

//  public static <V> Iterable<V> iterable(Iterator<V> itr) {
//    return NonReusableIterable.of(itr);
//  }

  public static <V> void reverse(Iterable<V> items, Consumer<V> callback) {
    if (items instanceof ReverseIterable && !(items instanceof NotReusable)) {
      final Iterable<V> iterable = ((ReverseIterable<V>) items).getIterable();
      if (iterable != null && !(iterable instanceof NotReusable)) {
        // we have a source iterable; let's recreate a new iterable.
        // NotReusable is to be used in the case of iterables which only wrap a single iterator; ie:
        // Iterable i = ()->iterator;
        // Instead, use X_Collect.iterable(iterator) to get a NotReusableIterable
        iterable.forEach(callback);
        return;
      }
    }
    // no luck on the iterable, try the iterator
    reverse(items.iterator(), callback);
  }

  public static <V> void reverse(Iterator<V> items, Consumer<V> callback) {
    if (items instanceof ReverseIterator && !(items instanceof NotReusable)) {
      final Iterable<V> iterable = ((ReverseIterator<V>) items).getIterable();
      if (iterable != null && !(iterable instanceof NotReusable)) {
        // we have a source iterable; let's recreate a new iterable.
        // NotReusable is to be used in the case of iterables which only wrap a single iterator; ie:
        // Iterable i = ()->iterator;
        // Instead, use X_Collect.iterable(iterator) to get a NotReusableIterable
        iterable.forEach(callback);
        return;
      }
    }
    new ReverseIterable<>(items).forEach(callback);
  }

  public static <T, G extends T> T[] toArray(Class<G> cls, Collection<T> items) {
    if (items == null) {
      return null;
    }
    return items.toArray((T[])Array.newInstance(cls, items.size()));
  }

  public static String[] toArray(Collection<String> items) {
    return toArray(String.class, items);
  }

  public static <K, Key extends K, V, Value extends V> CollectionProxy<K, V> newProxy(Class<Key> keyCls, Class<Value> valueCls, CollectionOptions opts) {
      return service.newProxy(keyCls, valueCls, opts);
  }

  /**
   * Despite the ugly mess of generic here,
   * this handy method can allow us to create
   * inline compute()-style methods like so:
   * <pre>
   *
   * </pre>
   * Map<String, Integer> m = new HashMap<>();
   * m.put("key", 0);
   * int i = computeMapTransform(m).io("key", (k, v)->v++);
   * assert i == 1;
   * assert map.get("key").equals(1);
   *
   */
  public static <Key, Val>
  In2Out1<Key, In2Out1<Key, Val, Val>, Val>
  computeMapTransform(Map<Key, Val> map) {
    return In2Out1.computeKeyValueTransform(map, Map::get, Map::put);
  }
}
