package xapi.collect.impl;

import xapi.api.marker.Trusted;
import xapi.collect.X_Collect;
import xapi.collect.api.CollectionOptions;
import xapi.collect.api.IntTo;
import xapi.collect.api.ObjectTo;
import xapi.collect.proxy.CollectionProxy;
import xapi.fu.In2Out1;
import xapi.util.impl.AbstractPair;

import java.lang.reflect.Array;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class IntToAbstract <V> implements IntTo<V> {

  private final CollectionProxy<Integer,V> store;
  private final CollectionOptions opts;
  private final Comparator<V> comparator;
  private boolean needsRebalance;
  private final Object lock = new Object();

  public IntToAbstract(CollectionProxy<Integer,V> store, CollectionOptions opts, Comparator<V> comparator) {
    this.store = store;
    this.opts = opts;
    this.comparator = comparator;
  }

  @Override
  public Iterable<V> forEach() {
    return store.iterateValues();
  }

  @Override
  public int size() {
    return store.size();
  }

  @Override
  public V[] toArray() {
    int s = size();
    V[] values = (V[]) Array.newInstance(valueType(), s);
    // TODO: consider sparse arrays?  Not very good for java (should use a map-backed implementation instead).
    while(s --> 0) {
      values[s] = get(s);
    }
    return values;
  }

  @Override
  public boolean add(V item) {
    maybeRebalance();
    assert valueType() == null || item == null || valueType().isAssignableFrom(item.getClass());
    return store.put(newEntry(size(), item)) == null;
  }

  @Override
  public boolean insert(int pos, V item) {
    maybeRebalance();
    do {
      item = store.put(newEntry(pos++, item));
    } while(item != null);
    return true;
  }

  protected Entry<Integer,V> newEntry(int size, V item) {
    return new AbstractPair<>(size, item);
  }

  public void push(V item) {
    maybeRebalance();
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
    if (into == null) {
      into = newList();
    }
    forEachValue(into::add);
    return into;
  }

  @Override
  public Map<Integer,V> toMap(Map<Integer,V> into) {
    if (into == null) {
      into = newMap();
    }
    forEachPair(into::put);
    return into;
  }

  protected Map<Integer, V> newMap() {
    return new LinkedHashMap<>();
  }

  @Override
  public ObjectTo<Integer,V> clone(CollectionOptions options) {
    final ObjectTo<Integer, V> cloned = X_Collect.newMap(Integer.class, valueType(), options);
    forEachPair(cloned::put);
    return cloned;
  }

  @Override
  public boolean contains(V value) {
    for (V val : forEach()) {
      if (comparator.compare(val, value)==0) {
        if (trustComparator(comparator)) {
          return true;
        }
        if (val == null ? value == null : val.equals(value)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean trustComparator(Comparator<V> comparator) {
    return comparator instanceof Trusted;
  }

  @Override
  public V at(int index) {
    maybeRebalance();
    return store.get(index);
  }

  @Override
  public int indexOf(V value) {
    maybeRebalance();
    for (int i = 0, s = store.size(); i < s; i++) {
      final V item = store.get(i);
      if (comparator.compare(item, value)==0) {
        if (!trustComparator(comparator)) {
          if (item == null ? value != null : !item.equals(value)) {
            continue;
          }
        }
        return i;
      }
    }
    return -1;
  }

  private void maybeRebalance() {
    if (needsRebalance) {
      if (opts.sparse()) {
        needsRebalance = false;
        return;
      }
      synchronized (lock) {
        if (needsRebalance) {
          needsRebalance = false;
          final V[] values = toArray();
          clear();
          addAll(values);
        }
      }
    }
  }

  @Override
  public boolean remove(int index) {
    if (store.remove(index) != null) {
      return (needsRebalance = true);
    }
    return false;
  }

  @Override
  public boolean findRemove(V value, boolean all) {
    if (all) {
      // When performing a removeAll, we will loop through backwards, and record if we performed any action
      boolean success = false;
      for (int i = store.size(); i --> 0; ) {
        final V item = store.get(i);
        if (comparator.compare(item, value)==0) {
          if (!trustComparator(comparator)) {
            if (item == null ? value != null : !item.equals(value)) {
              continue;
            }
          }
          store.remove(i);
          needsRebalance = true;
          success=true;
        }
      }
      return success;
    }
    // we are not removing all, just remove first...
    final int index = indexOf(value);
    return remove(index);
  }

  @Override
  public void set(int index, V value) {
    store.entryFor(index).setValue(value);
  }

  @Override
  public List<V> asList() {
    final List<V> list = newList();
    store.toCollection(list);
    return list;
  }

  protected List<V> newList() {
    return new ArrayList<>();
  }

  @Override
  public Set<V> asSet() {
    final Set<V> set = newSet();
    store.toCollection(set);
    return set;
  }

  protected Set<V> newSet() {
    return new LinkedHashSet<>();
  }

  @Override
  public Deque<V> asDeque() {
    final Deque<V> deque = newDeque();
    store.toCollection(deque);
    return deque;
  }

  protected Deque<V> newDeque() {
    return new ArrayDeque<>();
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
    needsRebalance = true;
    return store.remove(key);
  }

  @Override
  public boolean isEmpty() {
    return store.isEmpty();
  }

  @Override
  public void clear() {
    store.clear();
    needsRebalance = false;
  }

  @Override
  public Entry<Integer,V> entryFor(Object key) {
    maybeRebalance();
    return store.entryFor(key);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void setValue(Object key, Object value) {
    maybeRebalance();
    if (key instanceof Number) {
      set(((Number)key).intValue(), (V)value);
    } else {
      assert false : "Cannot use .setValue on a non-Number key " + key;
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
  public boolean readWhileTrue(In2Out1<Integer, V, Boolean> callback) {
    maybeRebalance();
    for (int i = 0, m = size(); i < m; i++ ) {
      if (!callback.io(i, get(i))) {
        return false;
      }
    }
    return true;
  }
}
