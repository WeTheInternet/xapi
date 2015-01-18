package xapi.collect.impl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import xapi.collect.X_Collect;
import xapi.collect.api.CollectionOptions;
import xapi.collect.api.IntTo;
import xapi.collect.api.ObjectTo;
import xapi.collect.api.StringTo;

public class IntToManyList <X> implements IntTo.Many<X>{

  private final StringTo.Many<X> map;
  private int max;

  public IntToManyList(Class<X> componentClass) {
    this.map = X_Collect.newStringMultiMap(componentClass, new TreeMap<String, IntTo<X>>());
  }

  @Override
  public Iterable<IntTo<X>> forEach() {
    return map.values();
  }

  @Override
  public boolean add(IntTo<X> item) {
    updateMax();
    map.put(Integer.toString(max++), item);
    return true;
  }

  private void updateMax() {
    while (map.containsKey(Integer.toString(max++))) {
      ;
    }
  }

  @Override
  public void add(int key, X item) {
    map.get(Integer.toString(key)).add(item);
  }

  @Override
  public boolean addAll(Iterable<IntTo<X>> items) {
    updateMax();
    for (IntTo<X> item : items) {
      map.put(Integer.toString(max++), item);
    }
    return true;
  }

  @Override
  public boolean addAll(IntTo<X>... items) {
    updateMax();
    for (IntTo<X> item : items) {
      map.put(Integer.toString(max++), item);
    }
    return true;
  }

  @Override
  public boolean insert(int pos, IntTo<X> item) {
    if (pos > max) {
      max = pos+1;
    }
    map.put(Integer.toString(pos), item);
    return false;
  }

  @Override
  public boolean contains(IntTo<X> value) {
    if (value == null) {
      return false;
    }
    main:
    for (IntTo<X> item : map.values()) {
      if (item.size() == value.size()) {
        for (int i = item.size(); i-->0;) {
          if (!equals(item.get(i), value.get(i))) {
            continue main;
          }
        }
        return true;
      }
    }
    return false;
  }

  private boolean equals(X x, X x2) {
    return x == null ? x2 == null : x.equals(x2);
  }

  @Override
  public IntTo<X> at(int index) {
    return map.get(Integer.toString(index));
  }

  @Override
  public int indexOf(IntTo<X> value) {
    if (value == null) {
      return -1;
    }
    String[] keys = map.keyArray();
    main:
    for (int i = keys.length; i-->0;) {
      String key = keys[i];
      IntTo<X> item = map.get(key);
      if (item.size() == value.size()) {
        for (int j = item.size(); j-->0;) {
          if (!equals(item.get(j), value.get(j))) {
            continue main;
          }
        }
        return Integer.parseInt(key);
      }
    }
    return -1;
  }

  @Override
  public boolean remove(int index) {
    return map.remove(Integer.toString(index)) != null;
  }

  @Override
  public boolean findRemove(IntTo<X> value, boolean all) {
    if (value == null) {
      return false;
    }
    boolean success = false;
    String[] keys = map.keyArray();
    main:
    for (int i = keys.length; i-->0;) {
      String key = keys[i];
      IntTo<X> item = map.get(key);
      if (item.size() == value.size()) {
        for (int j = item.size(); j-->0;) {
          if (!equals(item.get(j), value.get(j))) {
            continue main;
          }
        }
        if (!all) {
          return true;
        }
        success = true;
      }
    }
    return success;
  }

  @Override
  public void set(int index, IntTo<X> value) {
    if (index > max) {
      max = index+1;
    }
    map.put(Integer.toString(index), value);
  }

  @Override
  public void push(IntTo<X> value) {
    updateMax();
    map.put(Integer.toString(max++), value);
  }

  @Override
  public IntTo<X> pop() {
    updateMax();
    max--;
    IntTo<X> items = map.remove(Integer.toString(max));
    if (items != null) {
      return items;
    }
    String[] keys = map.keyArray();
    Arrays.sort(keys);
    return map.remove(keys[keys.length-1]);
  }

  @Override
  public List<IntTo<X>> asList() {
    List<IntTo<X>> list = newList();
    String[] keys = map.keyArray();
    Arrays.sort(keys);
    for (String key : keys) {
      list.add(map.get(key));
    }
    return list;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Set<IntTo<X>> asSet() {
    Set<IntTo<X>> set = newSet();
    if (map instanceof StringToAbstract) {
      set.addAll(((StringToAbstract<IntTo<X>>)map).valueSet());
    } else {
      for (IntTo<X> value : map.values()) {
        set.add(value);
      }
    }
    return set;
  }

  @Override
  public Deque<IntTo<X>> asDeque() {
    Deque<IntTo<X>> deque = newDeque();
    String[] keys = map.keyArray();
    Arrays.sort(keys);
    for (String key : keys) {
      deque.add(map.get(key));
    }
    return deque;
  }

  protected List<IntTo<X>> newList() {
    return new ArrayList<IntTo<X>>();
  }

  protected Set<IntTo<X>> newSet() {
    return new LinkedHashSet<IntTo<X>>();
  }

  protected Deque<IntTo<X>> newDeque() {
    return new ArrayDeque<IntTo<X>>();
  }

  protected Map<Integer, IntTo<X>> newMap() {
    return new TreeMap<Integer, IntTo<X>>();
  }

  @Override
  public ObjectTo<Integer, IntTo<X>> clone(CollectionOptions options) {
    throw new UnsupportedOperationException();
  }

  @Override
  public IntTo<X> put(Entry<Integer, IntTo<X>> item) {
    return map.put(Integer.toString(item.getKey()), item.getValue());
  }

  @Override
  public Entry<Integer, IntTo<X>> entryFor(Object key) {
    final String asString = String.valueOf(key);
    final int asInt = Integer.parseInt(asString);
    return new Entry<Integer, IntTo<X>>() {

      @Override
      public Integer getKey() {
        return asInt;
      }

      @Override
      public IntTo<X> getValue() {
        return map.get(asString);
      }

      @Override
      public IntTo<X> setValue(IntTo<X> value) {
        return map.put(asString, value);
      }
    };
  }

  @Override
  public IntTo<X> get(Object key) {
    assertValid(key);
    return map.get(String.valueOf(key));
  }

  private void assertValid(Object key) {
    assert Integer.parseInt(String.valueOf(key)) >= Integer.MIN_VALUE;
  }

  @Override
  public void setValue(Object key, Object value) {
    assertValid(key);
    if ((Integer)key > max) {
      max = (Integer)key;
    }
    map.put(String.valueOf(key), (IntTo<X>)value);
  }

  @Override
  public IntTo<X> remove(Object key) {
    assertValid(key);
    if (key.equals(max-1)) {
      max--;
    }
    return map.remove(String.valueOf(key));
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public IntTo<X>[] toArray() {
    updateMax();
    @SuppressWarnings("unchecked")
    IntTo<X>[] results = new IntTo[max];
    for (int i = max;i-->0;) {
      results[i] = map.get(String.valueOf(i));
    }
    assert noNegatives() : "Cannot use .toArray on an IntTo.Many with negative key values: "+Arrays.asList(map.keyArray());
    return results;
  }

  private boolean noNegatives() {
    for (String key : map.keyArray()) {
      if (Integer.parseInt(key) < 0) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Collection<IntTo<X>> toCollection(Collection<IntTo<X>> into) {
    if (into == null) {
      into = newList();
    }
    if (map instanceof StringToAbstract) {
      into.addAll(((StringToAbstract) map).valueSet());
    } else {
      String[] keys = map.keyArray();
      Arrays.sort(keys);
      for (String key : keys) {
        into.add(map.get(key));
      }
    }
    return into;
  }

  @Override
  public Map<Integer, IntTo<X>> toMap(Map<Integer, IntTo<X>> into) {
    if (into == null) {
      into = newMap();
    }
    for (String key : map.keyArray()) {
      into.put(Integer.parseInt(key), map.get(key));
    }
    return into;
  }

  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }

  @Override
  public void clear() {
    map.clear();
    max = 0;
  }


}
