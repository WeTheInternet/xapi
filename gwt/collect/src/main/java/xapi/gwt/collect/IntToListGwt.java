package xapi.gwt.collect;

import xapi.annotation.inject.InstanceOverride;
import xapi.collect.api.CollectionOptions;
import xapi.collect.api.IntTo;
import xapi.collect.api.ObjectTo;
import xapi.except.NotYetImplemented;
import xapi.platform.GwtPlatform;
import xapi.util.X_Util;
import xapi.util.api.ConvertsTwoValues;
import xapi.util.impl.AbstractPair;

import com.google.gwt.core.client.GwtScriptOnly;
import com.google.gwt.core.client.JavaScriptObject;

import javax.inject.Provider;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


@GwtPlatform
@InstanceOverride(implFor=IntTo.class)
@GwtScriptOnly
public class IntToListGwt <E> extends JavaScriptObject implements IntTo<E>{

  public static <V> IntTo<V> create(final Provider<V[]> arrayProvider) {
    return JavaScriptObject.createArray().<IntToListGwt<V>>cast().setArrayProvider(arrayProvider);
  }

  /**
   * This method is not safe for general use.  It is only to be used when you need to apply generics to a given type;
   * unfortunately, the bounds let you send any subtype (not just one with enhanced generics).
   */
  static <T, R extends T> IntTo<R> createForClass(final Class<T> cls) {
    return create(new Provider<R[]>() {
      @SuppressWarnings("unchecked")
      @Override
      public R[] get() {
        return (R[])Array.newInstance(cls, 0);
      }
    });
  }

  public static native <V> IntTo<V> newInstance()
  /*-{
   return [];
  }-*/;

  protected IntToListGwt() {}


  @Override
  public final boolean add(final E item) {
    set(size(), item);
    return true;
  }

  @Override
  @SuppressWarnings("unchecked")
  public final boolean addAll(final E... items) {
    for (final E item : items) {
      add(item);
    }
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
    this.splice(0, this.length);
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
  public final Iterable<E> forEach() {
    return new IntTo.IntToIterable<E>(this);
  }

  @Override
  public final E get(final Object key) {
    return getValue((Integer)key);
  }

  public final native E getValue(int key)
  /*-{
    return this[key]||null;
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
    while(this.length < pos){
      this.push(undefined);
    }
    this.splice(pos, 0, item);
    return true;
  }-*/;

  @Override
  public final boolean isEmpty() {
    return size()==0;
  }

  @Override
  public final native E pop()
  /*-{
    return this.splice(this.length-1, 1);
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
    while (this.length < index)
      this[this.length] = null;
    this[index] = value;
  }-*/;

  public final native IntToListGwt<E> setArrayProvider(Provider<E[]> provider)
  /*-{
     this.toArray = function(){return provider.@javax.inject.Provider::get()();}
     return this;
   }-*/;

  @Override
  @SuppressWarnings("unchecked")
  public final void setValue(final Object key, final Object value) {
    set((Integer)key, (E)value);
  }

  @Override
  public final native int size()
  /*-{
    return this.length;
  }-*/;

  public final native E splice(int key)
  /*-{
    if (key < this.length) {
      var ret = this[key];
      this.splice(key, 1);
      return ret;
    }
    return null;
  }-*/;


  @Override
  public final native E[] toArray()
  /*-{
    if (this.toArray) {
      var arr = this.toArray();
      for (var i in this) {
        if (i !== '_v$') {
          arr[i] = this[i];
        }
      }
      return arr;
    }
    throw "toArray not supported";
  }-*/;


  public final Class<Integer> keyType() {
    return Integer.class;
  }

  public final native Class<E> valueType()
  /*-{
    if (this._v$) {
        return this._v$;
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
      into = new LinkedHashMap<Integer, E>();
    }
    for (int i = 0, m = size(); i < m; i++) {
      into.put(i, getValue(i));
    }
    return into;
  }

  @Override
  public final boolean forEach(ConvertsTwoValues<Integer, E, Boolean> callback) {
    for (int i = 0, m = size(); i < m; i++ ) {
      if (!callback.convert(i, get(i))) {
        return false;
      }
    }
    return true;
  }

}
