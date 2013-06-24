package xapi.collect.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import xapi.collect.api.CollectionOptions;
import xapi.collect.api.IntTo;
import xapi.collect.api.ObjectTo;
import xapi.except.NotYetImplemented;
import xapi.util.X_Util;
import xapi.util.impl.AbstractPair;

public class IntToList<E> implements IntTo<E> {

  private final ArrayList<E> list = new ArrayList<E>(10);
  
  @Override
  public Iterable<E> forEach() {
    return list;
  }

  @Override
  public ObjectTo<Integer, E> clone(CollectionOptions options) {
    throw new NotYetImplemented("IntToList.clone not yet supported");
  }

  @Override
  public E put(Entry<Integer, E> item) {
    list.ensureCapacity(item.getKey());
    return list.set(item.getKey(), item.getValue());
  }

  @Override
  @SuppressWarnings("unchecked")
  public Entry<Integer, E> entryFor(Object key) {
    return new AbstractPair<Integer, E>(list.size(), (E)key);
  }

  @Override
  public E get(Object key) {
    return list.get((Integer)key);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void setValue(Object key, Object value) {
    int k = (Integer)key;
    list.ensureCapacity(k);
    list.set(k, (E)value);
  }

  @Override
  public E remove(Object key) {
    int k = (Integer)key;
    if (k < list.size())
      return list.remove(k);
    return null;
  }

  @Override
  public int size() {
    return list.size();
  }

  @Override
  @SuppressWarnings("unchecked")
  public E[] toArray() {
    return (E[])list.toArray();
  }

  @Override
  public Collection<E> toCollection(Collection<E> into) {
    if (into == null)
      into = newCollection();
    into.addAll(list);
    return into;
  }

  protected Collection<E> newCollection() {
    return new ArrayList<E>();
  }

  @Override
  public Map<Integer, E> toMap(Map<Integer, E> into) {
    if (into == null) {
      into = newMap();
    }
    ListIterator<E> iter = list.listIterator();
    while (iter.hasNext()) {
      into.put(iter.nextIndex(), iter.next());
    }
    return into;
  }

  private Map<Integer, E> newMap() {
    return new LinkedHashMap<Integer, E>();
  }

  @Override
  public boolean isEmpty() {
    return list.isEmpty();
  }

  @Override
  public void clear() {
    list.clear();
  }

  @Override
  public boolean add(E item) {
    return list.add(item);
  }

  @Override
  public boolean contains(E value) {
    return list.contains(value);
  }

  @Override
  public E at(int index) {
    if (index < list.size())
      return list.get(index);
    return null;
  }

  @Override
  public int indexOf(E value) {
    return list.indexOf(value);
  }

  @Override
  public boolean remove(int index) {
    if (index < list.size())
      return list.remove(index) != null;
    return false;
  }

  @Override
  public boolean findRemove(E value, boolean all) {
    ListIterator<E> iter = list.listIterator();
    boolean removed = false;
    while (iter.hasNext()) {
      E next = iter.next();
      if (X_Util.equal(next, value)) {
        iter.remove();
        if (!all)
          return true;
        removed = true;
      }
    }
    return removed;
  }

  @Override
  public void set(int index, E value) {
    list.ensureCapacity(index);
    list.set(index, value);
  }

  @Override
  public void push(E value) {
    list.add(value);
  }

  @Override
  public E pop() {
    if (list.size() == 0)
      return null;
    return list.remove(0);
  }

  @Override
  public List<E> asList() {
    ArrayList<E> list = new ArrayList<E>();
    list.addAll(this.list);
    return list;
  }

  @Override
  public Set<E> asSet() {
    Set<E> set = new LinkedHashSet<E>();
    set.addAll(list);
    return set;
  }

  @Override
  public Deque<E> asDeque() {
    LinkedList<E> deque = new LinkedList<E>();
    deque.addAll(list);
    return deque;
  }

}
