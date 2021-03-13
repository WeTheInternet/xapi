package xapi.collect.impl;

import xapi.collect.X_Collect;
import xapi.collect.api.CollectionOptions;
import xapi.collect.api.IntTo;
import xapi.collect.api.ObjectTo;
import xapi.collect.proxy.CollectionProxy;
import xapi.fu.In1;
import xapi.fu.In2Out1;
import xapi.fu.Out2;
import xapi.fu.X_Fu;
import xapi.util.impl.AbstractPair;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 5/21/16.
 */
public class IntToSet <V> implements IntTo<V>, Serializable {

  private final CollectionProxy<Integer,V> store;
  private final CollectionOptions opts;
  private boolean needsRebalance;
  private final Object lock = new Object();

  public <Generic extends V> IntToSet(
      Class<Generic> cls,
      CollectionOptions opts
  ) {
    opts = CollectionOptions.from(opts).forbidsDuplicate(true).build();
    this.store = X_Collect.newProxy(Integer.class, cls, opts);
    this.opts = opts;
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
    V[] values = newArray(s);
    final Iterator<V> itr = store.iterateValues().iterator();
    for(int i = 0; i < s; i++) {
      values[i] = itr.next();
    }
    return values;
  }

  private V[] newArray(int s) {
//    V[] arr = X_Fu.<V>array();
    V[] arr;
    try {
      arr = (V[]) Array.newInstance(store.valueType(), 0);
    } catch (Exception e) {
      arr = X_Fu.<V>array();
    }
    arr = X_Fu.blank(arr, s);
    return arr;
  }

  @Override
  public boolean add(V item) {
    maybeRebalance();
    assert valueType() == null || item == null || valueType().isAssignableFrom(item.getClass());
    if (opts.forbidsDuplicate()) {
      if (contains(item)) {
        return false;
      }
    }
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
    Collection<V> target = into;
    forEachValue(v->target.add(v));
    return into;
  }

  @Override
  public Map<Integer,V> toMap(Map<Integer,V> into) {
    if (into == null) {
      into = newMap();
    }
    Map<Integer, V> target = into;
    forEachPair(target::put);
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
    if (value == null) {
      for (V val : forEach()) {
        if (val == null) {
          return true;
        }
      }
    } else {
      for (V val : forEach()) {
          if (value.equals(val)) {
            return true;
        }
      }
    }
    return false;
  }

  @Override
  public V at(int index) {
    maybeRebalance();
    return store.get(index);
  }

  @Override
  public int indexOf(V value) {
    maybeRebalance();
    if (value == null) {
      for (int i = 0, s = store.size(); i < s; i++) {
        if (store.get(i) == null) {
          return i;
        }
      }
    } else {
      for (int i = 0, s = store.size(); i < s; i++) {
        final V item = store.get(i);
        if (value.equals(item)) {
              return i;
        }
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
    boolean success = false;
    if (value == null) {
      for (Out2<Integer, V> o : store.forEachEntry()) {
        V val = o.out2();
        if (val == null) {
          store.remove(o.out1());
          needsRebalance = true;
          if (all) {
            success = true;
          } else {
            return true;
          }
        }
      }
    } else {
      for (Out2<Integer, V> o : store.forEachEntry()) {
        V val = o.out2();
        if (value.equals(val)) {
          store.remove(o.out1());
          needsRebalance = true;
          if (all) {
            success = true;
          } else {
            return true;
          }
        }
      }

    }
    return success;
  }

  @Override
  public void set(int index, V value) {
    maybeRebalance();
    store.entryFor(index).setValue(value);
  }

  @Override
  public List<V> asList() {
    final List<V> list = newList();
    maybeRebalance();
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
    maybeRebalance();
    Entry<Integer,V> entry = store.entryFor(key);
    V current = entry.getValue();
    entry.setValue(value);
    return current;
  }

  @Override
  public V put(Entry<Integer,V> value) {
    maybeRebalance();
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
    boolean success = false;
    for (V item : items) {
      if (add(item)) {
        success = true;
      }
    }
    return success;
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

  @Override
  public String toString() {
    return toSource();
  }

  @Override
  public boolean removeValue(V value) {
    return IntTo.super.removeValue(value);
  }

  @Override
  public void removeAll(In1<V> callback) {
    IntTo.super.removeAll(callback);
  }

  @Override
  public String toString(Integer key, V value) {
    return IntTo.super.toString(key, value);
  }
}
