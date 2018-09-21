package xapi.gwt.collect;

import xapi.annotation.inject.InstanceOverride;
import xapi.collect.api.CollectionOptions;
import xapi.collect.api.IntTo;
import xapi.collect.api.ObjectTo;
import xapi.except.NotYetImplemented;
import xapi.fu.Filter.Filter1;
import xapi.fu.In1;
import xapi.fu.In2Out1;
import xapi.fu.itr.MappedIterable;
import xapi.fu.Out1;
import xapi.fu.Out2;
import xapi.fu.itr.SizedIterable;
import xapi.platform.GwtPlatform;
import xapi.util.X_Util;
import xapi.util.impl.AbstractPair;

import java.lang.reflect.Array;
import java.util.*;
import java.util.Map.Entry;

import com.google.gwt.core.client.GwtScriptOnly;
import com.google.gwt.core.client.JavaScriptObject;


@GwtPlatform
@InstanceOverride(implFor=IntTo.class)
@GwtScriptOnly
public class IntToListGwt <E> implements IntTo<E>{

  private final JavaScriptObject array;
  private Out1<E[]> provider;
  private Class<? extends E> valueCls;

  public IntToListGwt(JavaScriptObject array, Out1<E[]> arrayProvider) {
    this.array = array;
    this.provider = arrayProvider;
  }

  public static <V> IntToListGwt<V> create(final Out1<V[]> arrayProvider) {
    return new IntToListGwt<>(JavaScriptObject.createArray(), arrayProvider);
  }

  public static <V> IntToListGwt<V> createFrom(final JavaScriptObject array) {
    return new IntToListGwt<>(array);
  }

  public static <V> IntToListGwt<V> createFrom(final JavaScriptObject array, Out1<V[]> arrayProvider) {
    return new IntToListGwt<>(array, arrayProvider);
  }

  /**
   * This method is not safe for general use.  It is only to be used when you need to apply generics to a given type;
   * unfortunately, the bounds let you send any subtype (not just one with enhanced generics).
   */
  static <T, R extends T> IntTo<T> createForClass(final Class<R> cls) {
    final IntToListGwt<T> list = create(() -> (R[]) Array.newInstance(cls, 0));
    list.valueCls = cls;
    return list;
  }

  public static <V> IntTo<V> newInstance() {
    return new IntToListGwt<>(JavaScriptObject.createArray());
  }

  protected IntToListGwt(JavaScriptObject array) {
    this.array = array;
    this.provider = ()-> {
      throw new UnsupportedOperationException("IntToListGwt does not have an array provider" + this);
    };
  }

  @Override
  public final boolean add(final E item) {
    set(size(), item);
    return true;
  }

  @Override
  @SuppressWarnings("unchecked")
  public final boolean addAll(final E... items) {
    concat(items);
    return true;
  }

  @Override
  public final boolean addAll(final Iterable<E> items) {
    for (final E item : items) {
      add(item);
    }
    return true;
  }

  @Override
  public final boolean addAll(final IntTo<E> items) {
    if (X_Util.isArray(items)) {
      concat(items);
    } else {
      items.forEachValue(this::add);
    }
    return true;
  }

  @Override
  public final SizedIterable<E> forEachItem() {
    return IntTo.super.forEachItem();
  }

  @Override
  public final SizedIterable<Out2<Integer, E>> forEachEntry() {
    return IntTo.super.forEachEntry();
  }

  public final native void concat(Object array)
  /*-{
    this.@IntToListGwt::array.concat(array);
  }-*/;

  @Override
  public final Deque<E> asDeque() {
    final LinkedList<E> set = new LinkedList<E>();
    for (final E e : forEach()) {
      set.add(e);
    }
    return set;
  }
  @Override
  public final List<E> asList() {
    final ArrayList<E> list = new ArrayList<E>();
    for (final E e : forEach()) {
      list.add(e);
    }
    return list;
  }

  @Override
  public final Set<E> asSet() {
    final Set<E> set = new LinkedHashSet<E>();
    for (final E e : forEach()) {
      set.add(e);
    }
    return set;
  }

  @Override
  public final E at(final int index) {
    return getValue(index);
  }

  @Override
  public final native void clear()
  /*-{
    this.@IntToListGwt::array.splice(0, this.@IntToListGwt::array.length);
  }-*/;

  @Override
  public final ObjectTo<Integer, E> clone(final CollectionOptions options) {
    throw new NotYetImplemented("IntToList.clone not yet supported");
  }

  @Override
  public final boolean contains(final E value) {
    for (final E e : forEach()) {
      if (X_Util.equal(value, e)) {
        return true;
      }
    }
    return false;
  }

  @Override
  @SuppressWarnings("unchecked")
  public final Entry<Integer, E> entryFor(final Object key) {
    return new AbstractPair<Integer, E>(size(), (E)key);
  }

  @Override
  public final boolean removeIf(Filter1<E> value, boolean all) {
    return IntTo.super.removeIf(value, all);
  }

  @Override
  public final boolean findRemove(final E value, final boolean all) {
    boolean removed = false;
    for (int i = 0, m = size(); i < m; i++) {
      final E next = getValue(i);
      if (X_Util.equal(next, value)) {
        remove(i--);
        if (!all) {
          return true;
        }
        removed = true;
      }
    }
    return removed;
  }

  @Override
  public final MappedIterable<E> forEach() {
    return new IntToIterable<>(this);
  }

  @Override
  public void forEachValue(In1<E> callback) {
    forEach().forAll(callback);
  }

  /**
   * This method accepts Object for compatibility with jdk Collections as a Map,
   * but it expects an Integer object, and will fail if you send an Element E type.
   *
   */
  @Override
  @Deprecated
  public final E get(final Object key) {
    return getValue((Integer)key);
  }

  public final native E getValue(int key)
  /*-{
    return this.@IntToListGwt::array[key]||null;
  }-*/;

  @Override
  public final int indexOf(final E value) {
    for (int i = 0, m = size(); i < m; i++) {
      if (X_Util.equal(getValue(i), value)) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public final native boolean insert(int pos, E item)
  /*-{
    var arr = this.@IntToListGwt::array;
    while(arr.length < pos){
      arr.push(undefined);
    }
    arr.splice(pos, 0, item);
    return true;
  }-*/;

  @Override
  public final boolean isEmpty() {
    return size()==0;
  }

  @Override
  public final native E pop()
  /*-{
    return this.@IntToListGwt::array.splice(this.length-1, 1);
  }-*/;

  @Override
  public final void push(final E value) {
    set(size(), value);
  }

  @Override
  public final E put(final Entry<Integer, E> item) {
    set(item.getKey(), item.getValue());
    return item.getValue();
  }

  @Override
  public final boolean remove(final int index) {
    if (index < size()) {
      return splice(index) != null;
    }
    return false;
  }

  @Override
  public final E remove(final Object key) {
    return splice((Integer)key);
  }

  @Override
  public final native void set(int index, E value)
  /*-{
    var arr = this.@IntToListGwt::array;
    while (arr.length < index)
      arr[arr.length] = null;
    arr[index] = value;
  }-*/;

  public final IntToListGwt<E> setArrayProvider(Out1<E[]> provider) {
    this.provider = provider;
    return this;
  }

  @Override
  @SuppressWarnings("unchecked")
  public final void setValue(final Object key, final Object value) {
    set((Integer)key, (E)value);
  }

  @Override
  public final native int size()
  /*-{
    return this.@IntToListGwt::array.length;
  }-*/;

  public final native E splice(int key)
  /*-{
    var arr = this.@IntToListGwt::array;
    if (key < arr.length) {
      var ret = arr[key];
      arr.splice(key, 1);
      return ret;
    }
    return null;
  }-*/;


  @Override
  public final native E[] toArray()
  /*-{
    var provider = this.@IntToListGwt::provider;
    var source = this.@IntToListGwt::array;
    var arr = provider.@Out1::out1()();
    for (var i in source) {
        arr[i] = source[i];
    }
    return arr;
  }-*/;


  public final Class<Integer> keyType() {
    return Integer.class;
  }

  public final native Class<E> valueType()
  /*-{
    if (this.@IntToListGwt::valueCls) {
        return this.@IntToListGwt::valueCls;
    }
    return @IntToListGwt::guessValueClass(*)(this);
  }-*/;

  private static final <E> Class<E> guessValueClass(IntToListGwt<E> from) {
    // Brutal, but effective
    return Class.class.cast(from.toArray().getClass().getComponentType());
  }

  @Override
  public final Collection<E> toCollection(Collection<E> into) {
    if (into == null) {
      into = new ArrayList<E>();
    }
    for (final E e : forEach()) {
      into.add(e);
    }
    return into;
  }

  @Override
  public final Map<Integer, E> toMap(Map<Integer, E> into) {
    if (into == null) {
      into = new LinkedHashMap<>();
    }
    for (int i = 0, m = size(); i < m; i++) {
      into.put(i, getValue(i));
    }
    return into;
  }

  @Override
  public final boolean readWhileTrue(In2Out1<Integer, E, Boolean> callback) {
    for (int i = 0, m = size(); i < m; i++ ) {
      if (!callback.io(i, get(i))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public final String toString(Integer key, E value) {
    return String.valueOf(value);
  }

  public JavaScriptObject rawArray() {
    return array;
  }
}
