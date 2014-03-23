package xapi.gwt.collect;

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

import javax.inject.Provider;

import xapi.annotation.inject.InstanceOverride;
import xapi.collect.api.CollectionOptions;
import xapi.collect.api.IntTo;
import xapi.collect.api.ObjectTo;
import xapi.except.NotYetImplemented;
import xapi.platform.GwtPlatform;
import xapi.util.X_Util;
import xapi.util.impl.AbstractPair;

import com.google.gwt.core.client.GwtScriptOnly;
import com.google.gwt.core.client.JavaScriptObject;


@GwtPlatform
@InstanceOverride(implFor=IntTo.class)
@GwtScriptOnly
public class IntToListGwt <E> extends JavaScriptObject implements IntTo<E>{

  protected IntToListGwt() {}
  

  public static native <V> IntTo<V> newInstance()
  /*-{
   return [];
  }-*/;

  public static <V> IntTo<V> create(Provider<V[]> arrayProvider) {
    return JavaScriptObject.createArray().<IntToListGwt<V>>cast().setArrayProvider(arrayProvider);
  }
  
  
  @Override
  public final ObjectTo<Integer, E> clone(CollectionOptions options) {
    throw new NotYetImplemented("IntToList.clone not yet supported");
  }

  @Override
  public final E put(Entry<Integer, E> item) {
    set(item.getKey(), item.getValue());
    return item.getValue();
  }

  @Override
  @SuppressWarnings("unchecked")
  public final Entry<Integer, E> entryFor(Object key) {
    return new AbstractPair<Integer, E>(size(), (E)key);
  }

  @Override
  public final E get(Object key) {
    return getValue((Integer)key);
  }
  public final native E getValue(int key)
  /*-{
    return this[key]||null;
  }-*/;

  @Override
  @SuppressWarnings("unchecked")
  public final void setValue(Object key, Object value) {
    set((Integer)key, (E)value);
  }

  @Override
  public final E remove(Object key) {
    return splice((Integer)key);
  }

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
  public final native int size()
  /*-{
    return this.length;
  }-*/;
  
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
  public final native E[] toArray()
  /*-{
    if (this.toArray) {
      var arr = this.toArray();
      this.forEach(function(i) {
        arr[arr.length] = i;
      });
      return arr;
    }
    throw "toArray not supported";
  }-*/;
  
  public final native IntToListGwt<E> setArrayProvider(Provider<E[]> provider)
  /*-{
     this.toArray = function(){return provider.@javax.inject.Provider::get()();}
     return this;
   }-*/;

  @Override
  public final Collection<E> toCollection(Collection<E> into) {
    if (into == null)
      into = new ArrayList<E>();
    for (E e : forEach()) {
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
  public final boolean isEmpty() {
    return size()==0;
  }

  @Override
  public final native void clear()
  /*-{
    this.splice(0, this.length);
  }-*/;

  @Override
  public final Iterable<E> forEach() {
    return new IntTo.IntToIterable<E>(this);
  }

  @Override
  public final boolean add(E item) {
    set(size(), item);
    return true;
  }

  @Override
  public final boolean contains(E value) {
    for (E e : forEach()) {
      if (X_Util.equal(value, e))
        return true;
    }
    return false;
  }

  @Override
  public final E at(int index) {
    return getValue(index);
  }

  @Override
  public final int indexOf(E value) {
    for (int i = 0, m = size(); i < m; i++) {
      if (X_Util.equal(getValue(i), value))
        return i;
    }
    return -1;
  }

  @Override
  public final boolean remove(int index) {
    if (index < size()) {
      return splice(index) != null;
    }
    return false;
  }

  @Override
  public final boolean findRemove(E value, boolean all) {
    boolean removed = false;
    for (int i = 0, m = size(); i < m; i++) {
      E next = getValue(i);
      if (X_Util.equal(next, value)) {
        remove(i--);
        if (!all)
          return true;
        removed = true;
      }
    }
    return removed;
  }

  @Override
  public final native void set(int index, E value)
  /*-{
    while (this.length < index)
      this[this.length] = null;
    this[index] = value;
  }-*/;

  @Override
  public final void push(E value) {
    set(size(), value);
  }

  @Override
  public final native E pop()
  /*-{
    return this.splice(this.length-1, 1);
  }-*/;


  @Override
  public final List<E> asList() {
    ArrayList<E> list = new ArrayList<E>();
    for (E e : forEach())
      list.add(e);
    return list;
  }

  @Override
  public final Set<E> asSet() {
    Set<E> set = new LinkedHashSet<E>();
    for (E e : forEach())
      set.add(e);
    return set;
  }

  @Override
  public final Deque<E> asDeque() {
    LinkedList<E> set = new LinkedList<E>();
    for (E e : forEach())
      set.add(e);
    return set;
  }

}
