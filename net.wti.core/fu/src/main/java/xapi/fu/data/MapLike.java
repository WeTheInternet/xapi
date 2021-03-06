package xapi.fu.data;

import xapi.fu.Filter.Filter1;
import xapi.fu.Filter.Filter2;
import xapi.fu.*;
import xapi.fu.Out1.Out1Unsafe;
import xapi.fu.api.Ignore;
import xapi.fu.has.HasLock;
import xapi.fu.itr.ArrayIterable;
import xapi.fu.itr.EmptyIterator;
import xapi.fu.itr.SizedIterable;
import xapi.fu.itr.SizedIterator;

import java.util.Iterator;

import static xapi.fu.itr.EmptyIterator.NONE;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/10/16.
 */
@Ignore("model") // a model that implements MapLike will be implementing all methods itself; ignore anything defined in this type.
public interface MapLike<K, V> extends CollectionLike<Out2<K, V>> {
//public interface MapLike<K, V> extends HasSize, HasItems<Out2<K, V>>, Clearable {

  /**
   * A put operation.  Returns the previous value, if any.
   */
  V put(K key, V value);

  /**
   * A get operation.  Returns the known value, if any.
     */
  V get(K key);

  /**
   * A get operation, surrounding in a {@link Maybe} facade,
   * for functional fallbacks.
   *
   * THIS METHOD READS THE MAP ONCE AND IS THEN IMMUTABLE.
   *
   * For a mutable maybe bound to this map, see {@link #getMaybeBound(K)}
   *
   * Example:
   *
   boolean hasKey(String key) {
       map.getMaybe(key)
       .mapNullSafe(p->p.getKeys().contains(key))
       .isPresent();
   }

   or, if you will...
       maybe.mapNullSafe(MyType::getKeys)
            .filter(MapLike::contains, key)
            .isPresent();
   */
  default Maybe<V> getMaybe(K key) {
    return Maybe.nullable(get(key));
  }

  default V putFromValue(V value, In1Out1<V, K> keyFinder) {
    final K key = keyFinder.io(value);
    return put(key, value);
  }

  default MapLike<K, V> putFromValuesItr(Iterable<V> value, In1Out1<V, K> keyFinder) {
    for (V v : value) {
      putFromValue(v, keyFinder);
    }
    return this;
  }
  default MapLike<K, V> putFromValues(In1Out1<V, K> keyFinder, V ... values) {
    putFromValuesItr(ArrayIterable.iterate(values), keyFinder);
    return this;
  }

  /**
   * Returns a Maybe that is bound to the underlying data of this map
   * (i.e., the internal state can change from present to absent on you.
   *
   * If you want an immutable snapshot, either call .lazy() on the resulting Maybe,
   * or use {@link #getMaybe(K)}
   */
  default Maybe<V> getMaybeBound(K key) {
    return ()->get(key);
  }
  /**
   * Check if there is an entry for the given key.
   */
  boolean has(K key);

  default boolean use(K key, In1<V> callback) {
    if (has(key)) {
      callback.in(get(key));
      return true;
    }
    return false;
  }

  @Override
  default MapLike<K, V> add(Out2<K, V> value) {
    putPair(value);
    return this;
  }

  /**
   * A remove operation.  Returns the deleted value, if any.
     */
  V remove(K key);

  default Maybe<V> removeMaybe(K key) {
    return Maybe.nullable(remove(key));
  }

  SizedIterable<K> keys();

  @Override
  default SizedIterable<Out2<K, V>> forEachItem() {
    return this;
  }

  default V[] removeAllValues(In1Out1<Integer, V[]> valueFactory) {
    return HasLock.maybeLock(this, ()->{
      V[] values = forEachItem()
          .map(Out2::out2)
          .filterNull()
          .toArray(valueFactory);
      clear();
      return values;
    });
  }

  default K[] removeAllKeys(In1Out1<Integer, K[]> keyFactory) {
    return HasLock.maybeLock(this, ()->{
      K[] keys = forEachItem()
          .map(Out2::out1)
          .filterNull()
          .toArray(keyFactory);
      clear();
      return keys;
    });
  }

  default Out2<K, V>[] removeAllItems() {
    return HasLock.maybeLock(this, ()->{
      final Out2<K, V>[] keys = forEachItem()
          .filterNull()
          .toArray(Out2[]::new);
      clear();
      return keys;
    });
  }

  default SizedIterable<V> mappedValues() {
    return map(Out2::out2);
  }

  default V getOrCreate(K key, In1Out1<K, V> ifNull) {
    V is = get(key);
    if (is == null) {
      is = ifNull.io(key);
      put(key, is);
    }
    return is;
  }

  default V getOrCreateFrom(K key, Out1<V> ifNull) {
    return HasLock.alwaysLock(this, ()->{
      V is = get(key);
      if (is == null) {
        is = ifNull.out1();
        put(key, is);
      }
      return is;
    });
  }
  default <F> V getOrCreateFrom(K key, In1Out1<F, V> ifNull, F from) {
    return getOrCreateFrom(key, ifNull.supply(from));
  }

  default <F1, F2> V getOrCreateFrom(K key, In2Out1<F1, F2, V> ifNull, F1 from1, F2 from2) {
    return getOrCreateFrom(key, ifNull.supply1(from1).supply(from2));
  }

  default <F1, F2, F3> V getOrCreateFrom(K key, In3Out1<F1, F2, F3, V> ifNull, F1 from1, F2 from2, F3 from3) {
    return getOrCreateFrom(key, ifNull.supply1(from1).supply1(from2).supply(from3));
  }

  default V getOrSupply(K key, Out1<V> ifNull) {
    V is = get(key);
    if (is == null) {
      return ifNull.out1();
    }
    return is;
  }

  default V getOrSupplyUnsafe(K key, Out1Unsafe<V> ifNull) {
    return getOrSupply(key, ifNull);
  }

  default V getAndRemove(K key) {
    final V was = get(key);
    if (was != null) {
      remove(key);
    }
    return was;
  }

  default Maybe<V> getAndRemoveMaybe(K key) {
    final V was = get(key);
    if (was == null) {
      return Maybe.not();
    }
    remove(key);
    return Maybe.immutable(was);
  }

  default V getOrReturn(K key, V ifNull) {
    V is = get(key);
    if (is == null) {
      return ifNull;
    }
    return is;
  }

  default Maybe<V> removeIfBoth(K key, Filter1<K> filter) {
    if (filter.filter1(key)) {
      return Maybe.immutable(remove(key));
    }
    return Maybe.not();
  }

  default Maybe<V> removeIfKey(K key, Filter1<K> filter) {
    if (filter.filter1(key)) {
      return Maybe.immutable(remove(key));
    }
    return Maybe.not();
  }

  default Maybe<V> removeIfValue(K key, Filter1<V> filter) {
    final V value = get(key);
    if (filter.filter1(value)) {
      return Maybe.immutable(remove(key));
    }
    return Maybe.not();
  }

  default V mapBoth(K key, In2Out1<K, V, V> mapper) {
      final V is = get(key);
      final V newValue = mapper.io(key, is);
      return put(key, newValue);
  }

  default V mapKey(K key, In1Out1<K, V> mapper) {
    final V newValue = mapper.io(key);
    return put(key, newValue);
  }

  default V mapValue(K key, In1Out1<V, V> mapper) {
    final V is = get(key);
    final V newValue = mapper.io(is);
    return put(key, newValue);
  }

  default <NewKey> MapLike<NewKey, V> mapKey(In1Out1<NewKey, K> fromNewType, In1Out1<K, NewKey> toNewType) {
    final MapLike<K, V> self = this;
    return new MapLike<NewKey, V>() {
      @Override
      public SizedIterator<Out2<NewKey, V>> iterator() {
        return SizedIterator.of(self.iterator(), o->{
          final Out1<NewKey> key = toNewType.supplyDeferred(o.out1Provider());
          return Out2.out2(key, o.out2());
        });
      }

      @Override
      public V put(NewKey key, V value) {
        final K mappedKey = fromNewType.io(key);
        return self.put(mappedKey, value);
      }

      @Override
      public V get(NewKey key) {
        final K mappedKey = fromNewType.io(key);
        return self.get(mappedKey);
      }

      @Override
      public boolean has(NewKey key) {
        final K mappedKey = fromNewType.io(key);
        return self.has(mappedKey);
      }

      @Override
      public V remove(NewKey key) {
        final K mappedKey = fromNewType.io(key);
        return self.remove(mappedKey);
      }

      @Override
      public SizedIterable<NewKey> keys() {
        return self.keys().map(toNewType);
      }

      @Override
      public void clear() {
        self.clear();
      }

      @Override
      public int size() {
        return self.size();
      }
    };
  }

  default <NewValue> MapLike<K, NewValue> mapValue(In2Out1<K, NewValue, V> fromNewType, In1Out1<V, NewValue> toNewType) {
    final MapLike<K, V> self = this;
    return new MapLike<K, NewValue>() {
      @Override
      public SizedIterator<Out2<K, NewValue>> iterator() {
        return SizedIterator.of(self.iterator(), o->{
          final Out1<NewValue> value = toNewType.supplyDeferred(o.out2Provider());
          return Out2.out2(o.out1Provider(), value);
        });
      }

      @Override
      public NewValue put(K key, NewValue value) {
        final V mappedValue = fromNewType.io(key, value);
        final V was = self.put(key, mappedValue);
        if (was == null) {
          return null;
        }
        final NewValue mappedWas = toNewType.io(was);
        return mappedWas;
      }

      @Override
      public NewValue get(K key) {
        final V value = self.get(key);
        if (value == null) {
          return null;
        }
        final NewValue mappedValue = toNewType.io(value);
        return mappedValue;
      }

      @Override
      public boolean has(K key) {
        return self.has(key);
      }

      @Override
      public NewValue remove(K key) {
        final V was = self.remove(key);
        if (was == null) {
          return null;
        }
        final NewValue mappedValue = toNewType.io(was);
        return mappedValue;
      }

      @Override
      public SizedIterable<K> keys() {
        return self.keys();
      }

      @Override
      public void clear() {
        self.clear();
      }

      @Override
      public int size() {
        return self.size();
      }
    };
  }

  default V filterKeyMapBoth(K key, Filter1<K> filter, In2Out1<K, V, V> mapper) {
    if (filter.filter1(key)) {
      final V is = get(key);
      final V newValue = mapper.io(key, is);
      return put(key, newValue);
    } else {
      return get(key);
    }
  }

  default V filterKeyMapKey(K key, Filter1<K> filter, In1Out1<K, V> mapper) {
    if (filter.filter1(key)) {
      final V newValue = mapper.io(key);
      return put(key, newValue);
    } else {
      return get(key);
    }
  }

  default V findByKey(Filter1<K> filter) {
    K key;
    for (Out2<K, V> item : this) {
      key = item.out1();
      if (filter.filter1(key)) {
        return item.out2();
      }
    }
    return null;
  }

  default V findByValue(Filter1<V> filter) {
    V value;
    for (Out2<K, V> item : this) {
      value = item.out2();
      if (filter.filter1(value)) {
        return value;
      }
    }
    return null;
  }

  default V filterKeyMapValue(K key, Filter1<K> filter, In1Out1<V, V> mapper) {
    final V is = get(key);
    if (filter.filter1(key)) {
      final V newValue = mapper.io(is);
      return put(key, newValue);
    }
    return is;
  }

  default V filterValueMapBoth(K key, Filter1<V> filter, In2Out1<K, V, V> mapper) {
    final V is = get(key);
    if (filter.filter1(is)) {
      final V newValue = mapper.io(key, is);
      return put(key, newValue);
    } else {
      return get(key);
    }
  }

  default V filterValueMapKey(K key, Filter1<V> filter, In1Out1<K, V> mapper) {
    final V newValue = mapper.io(key);
    if (filter.filter1(newValue)) {
      return put(key, newValue);
    } else {
      return get(key);
    }
  }

  default V filterOldValueMapValue(K key, Filter1<V> filter, In1Out1<V, V> mapper) {
    final V is = get(key);
    if (filter.filter1(is)) {
    final V newValue = mapper.io(is);
      return put(key, newValue);
    }
    return is;
  }

  default V filterNewValueMapValue(K key, Filter1<V> filter, In1Out1<V, V> mapper) {
    final V is = get(key);
    final V newValue = mapper.io(is);
    if (filter.filter1(newValue)) {
      return put(key, newValue);
    }
    return is;
  }

  // TODO: rename all these to ifKey*Compute*Both
  default MapLike<K, V> ifKeyMapBoth(K key, Filter1<K> filter, In2Out1<K, V, V> mapper) {
    filterKeyMapBoth(key, filter, mapper);
    return this;
  }

  default MapLike<K, V> ifKeyMapKey(K key, Filter1<K> filter, In1Out1<K, V> mapper) {
    filterKeyMapKey(key, filter, mapper);
    return this;
  }

  default MapLike<K, V> ifKeyMapValue(K key, Filter1<K> filter, In1Out1<V, V> mapper) {
    filterKeyMapValue(key, filter, mapper);
    return this;
  }

  default MapLike<K, V> ifValueMapBoth(K key, Filter1<V> filter, In2Out1<K, V, V> mapper) {
    filterValueMapBoth(key, filter, mapper);
    return this;
  }

  default MapLike<K, V> ifValueMapKey(K key, Filter1<V> filter, In1Out1<K, V> mapper) {
    filterValueMapKey(key, filter, mapper);
    return this;
  }

  default MapLike<K, V> ifOldValueMapValue(K key, Filter1<V> filter, In1Out1<V, V> mapper) {
    filterOldValueMapValue(key, filter, mapper);
    return this;
  }

  default MapLike<K, V> ifNewValueMapValue(K key, Filter1<V> filter, In1Out1<V, V> mapper) {
    filterNewValueMapValue(key, filter, mapper);
    return this;
  }

  default MapLike<K, V> ifBothMapBoth(K key, Filter2<Object, K, V> filter, In2Out1<K, V, V> mapper) {
    final V value = get(key);
    if (filter.filter2(key, value)) {
      final V newValue = mapper.io(key, value);
      put(key, newValue);
    }
    return this;
  }

  default Out2<V, V> putAndReturnBoth(K key, V value) {
    return Out2.out2Immutable(put(key, value), value);
  }

  default Out2<V, V> putIfUnchanged(K key, V previousValue, V value) {
    if (previousValue == get(key)) {
      return putAndReturnBoth(key, value);
    } else {
      value = previousValue;
    }
    return Out2.out2Immutable(previousValue, value);
  }

  default V compute(K key, In2Out1<K, V, V> io) {
    return HasLock.alwaysLock(this, ()->{
      V existing = get(key);
      final V computed = io.io(key, existing);
      if (computed != existing) {
        put(key, computed);
      }
      return computed;
    });
  }


  default V compute(K key, In1<V> ifPresent, Out1<V> ifAbsent) {
    return HasLock.alwaysLock(this, ()->{
      V value = get(key);
      if (value == null) {
        value = ifAbsent.out1();
        put(key, value);
      } else {
        ifPresent.in(value);
      }
      return value;
    });
  }

  default V computeIfAbsent(K key, Out1<V> ifAbsent) {
    return compute(key, In1.ignored(), ifAbsent);
  }

  default V computeIfAbsent(K key, In1Out1<K, V> ifAbsent) {
    return compute(key, (k, v)-> {
      if (v == null) {
        v = ifAbsent.io(key);
        put(key, v);
      }
      return v;
    });
  }

  default V computeValue(K key, In1Out1<V, V> io) {
    return HasLock.alwaysLock(this, ()->{
      V existing = get(key);
      final V computed = io.io(existing);
      if (computed != existing) {
        put(key, computed);
      }
      return computed;
    });
  }

  default V computeReturnPrevious(K key, In2Out1<K, V, V> io) {
    return HasLock.alwaysLock(this, ()-> {
      V existing = get(key);
      final V computed = io.io(key, existing);
      if (computed != existing) {
        put(key, computed);
      }
      return existing;
    });
  }

  @Override
  int size();

  MapLike EMPTY = new MapLike<Object, Object>() {

    @Override
    public SizedIterator iterator() {
      return EmptyIterator.EMPTY;
    }

    @Override
    public void clear() {
    }

    @Override
    public int size() {
      return 0;
    }

    @Override
    public Object put(Object key, Object value) {
      throw new UnsupportedOperationException("EMPTY");
    }

    @Override
    public Object get(Object key) {
      return null;
    }

    @Override
    public boolean has(Object key) {
      return false;
    }

    @Override
    public Object remove(Object key) {
      return null;
    }

    @Override
    public SizedIterable<Object> keys() {
      return NONE;
    }
  };

  static <K, V> MapLike<K, V> empty() {
    return EMPTY;
  }

  default void putMap(MapLike<K, V> into) {
    into.forEachItem().forAll(this::putPair);
  }

  default void putPair(Out2<K, V> pair) {
    if (pair != null) {
      put(pair.out1(), pair.out2());
    }
  }

  default Maybe<Out2<K,V>> firstMaybe() {
    final Iterator<Out2<K, V>> items = forItems(1).iterator();
    if (items.hasNext()) {
      return Maybe.immutable(items.next());
    }
    return Maybe.not();
  }

  default Maybe<Out2<K,V>> lastMaybe() {
    final SizedIterable keys = keys();
    if (keys.isEmpty()) {
      return Maybe.not();
    }
    // We don't know the class K (for our keys), but we don't really need to...
    final Object[] items = keys.toArray(Object.class); // Avoids O(n) lookups if keys() is backed by something
    // which can optimally supply us with keys in array format...
    final K key = (K)items[items.length - 1];
    return Maybe.immutable(Immutable.immutable2(key, get(key)));
  }

  default <To> Maybe<To> findAndMap(In2Out1<K, V, To> filter) {
    for (Out2<K, V> item : forEachItem()) {
      To result = filter.io(item.out1(), item.out2());
      if (result != null) {
        return Maybe.immutable(result);
      }
    }
    return Maybe.not();
  }
}
