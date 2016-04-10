package xapi.collect.impl;

import xapi.collect.api.CollectionOptions;
import xapi.collect.api.IntTo;
import xapi.collect.api.ObjectTo;
import xapi.collect.proxy.CollectionProxy;
import xapi.except.NotYetImplemented;
import xapi.util.api.ConvertsTwoValues;

import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class IntToAbstract <V> implements IntTo<V> {

  private final CollectionProxy<Integer,V> store;
  private final CollectionOptions opts;
  private final Comparator<V> comparator;

  public IntToAbstract(CollectionProxy<Integer,V> store, CollectionOptions opts
    , Comparator<V> comparator) {
    this.store = store;
    this.opts = opts;
    this.comparator = comparator;
  }

  @Override
  public Iterable<V> forEach() {
    return new Iterable<V>() {
      @Override
      public Iterator<V> iterator() {
        return new IntToIterator<V>(IntToAbstract.this);
      }
    };
  }
  @Override
  public int size() {
    return store.size();
  }

  @Override
  public V[] toArray() {
    return null;
  }

  @Override
  public boolean add(V item) {
    assert valueType() == null || item == null || valueType().isAssignableFrom(item.getClass());
    return store.put(newEntry(size(), item)) == null;
  }

  @Override
  public boolean insert(int pos, V item) {
    for (int i = store.size(); i --> 0; ) {
      V v = store.get(i);
      store.put(newEntry(i+1, v));
    }
    store.put(newEntry(pos, item));
    return true;
  }

  protected Entry<Integer,V> newEntry(int size, V item) {
    return null;
  }

  public void push(V item) {
    store.put(newEntry(size(), item));
  }

  public V pop() {
    int size = size();
    if (size > 0) {
      try {
        return get(--size);
      }finally {
        remove(size);
      }
    }
    return null;
  }

  @Override
  public Collection<V> toCollection(Collection<V> into) {
    throw new NotYetImplemented(getClass() + " must implement toCollection");
  }

  @Override
  public Map<Integer,V> toMap(Map<Integer,V> into) {
    throw new NotYetImplemented(getClass() + " must implement toMap");
  }

  @Override
  public ObjectTo<Integer,V> clone(CollectionOptions options) {
    throw new NotYetImplemented(getClass() + " must implement clone");
  }

  @Override
  public boolean contains(V value) {
    for (V val : forEach()) {
      if (comparator.compare(val, value)==0)
        return true;
    }
    return false;
  }

  @Override
  public V at(int index) {
    return store.get(index);
  }

  @Override
  public int indexOf(V value) {
    for (int i = 0, s = store.size(); i < s; i++) {
      if (comparator.compare(store.get(i), value)==0)
        return i;
    }
    return -1;
  }

  @Override
  public boolean remove(int index) {
    return store.remove(index) != null;
  }

  @Override
  public boolean findRemove(V value, boolean all) {
    boolean success = false;
    for (int i = 0, s = store.size(); i < s; i++) {
      if (comparator.compare(store.get(i), value)==0) {
        store.remove(i);
        s = store.size();
        i--;
        if (all)
          success=true;
        else
          return true;
      }
    }
    return success;
  }

  @Override
  public void set(int index, V value) {
    store.entryFor(index).setValue(value);
  }

  @Override
  public List<V> asList() {
    return null;
  }

  @Override
  public Set<V> asSet() {
    return null;
  }

  @Override
  public Deque<V> asDeque() {
    return null;
  }

  @Override
  public V get(Object key) {
    return store.get(key);
  }

  public V put(int key, V value) {
    Entry<Integer,V> entry = store.entryFor(key);
    V current = entry.getValue();
    entry.setValue(value);
    return current;
  }

  @Override
  public V put(Entry<Integer,V> value) {
    return store.put(value);
  }

  @Override
  public V remove(Object key) {
    return store.remove(key);
  }

  @Override
  public boolean isEmpty() {
    return store.isEmpty();
  }

  @Override
  public void clear() {
    store.clear();
  }

  @Override
  public Entry<Integer,V> entryFor(Object key) {
    return store.entryFor(key);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void setValue(Object key, Object value) {
    if (key instanceof Number) {
      set(((Number)key).intValue(), (V)value);
    }
  }

  @Override
  public boolean addAll(Iterable<V> items) {
    for (V item : items) {
      add(item);
    }
    return true;
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean addAll(V ... items) {
    for (V item : items) {
      add(item);
    }
    return true;
  }

  public Class<Integer> keyType() {
    return Integer.class;
  }

  public Class<V> valueType() {
    return store.valueType();
  }

  @Override
  public boolean forEach(ConvertsTwoValues<Integer, V, Boolean> callback) {
    for (int i = 0, m = size(); i < m; i++ ) {
      if (!callback.convert(i, get(i))) {
        return false;
      }
    }
    return true;
  }
}
