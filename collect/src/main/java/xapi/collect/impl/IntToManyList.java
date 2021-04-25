package xapi.collect.impl;

import xapi.collect.X_Collect;
import xapi.collect.api.CollectionOptions;
import xapi.collect.api.IntTo;
import xapi.collect.api.ObjectTo;
import xapi.collect.proxy.impl.MapOf;
import xapi.fu.In2Out1;
import xapi.fu.Out2;

import java.util.*;
import java.util.Map.Entry;

public class IntToManyList <X> implements IntTo.Many<X>{

  private final ObjectTo.Many<Integer, X> map;
  private final Class<X> componentClass;
  private int max;

  public IntToManyList(final Class<X> componentClass) {
    this(componentClass, CollectionOptions.asKeyOrdered().build());
  }

  public IntToManyList(Class<X> componentClass, CollectionOptions opts) {
    this.map = X_Collect.newMultiMap(Integer.class, componentClass, opts);
    this.componentClass = componentClass;
  }

  @Override
  public Iterable<IntTo<X>> forEach() {
    return map.values();
  }

  public Class<Integer> keyType() {
    return Integer.class;
  }

  public Class<IntTo<X>> valueType() {
    return Class.class.cast(IntTo.class);
  }

  @Override
  public boolean add(final IntTo<X> item) {
    updateMax();
    map.put(max++, item);
    return true;
  }

  private void updateMax() {
    // TODO limit this brute force approach so that sparse arrays don't suffer
    while (map.containsKey(max++)) {
      ;
    }
  }

  @Override
  public void add(final int key, final X item) {
    assert item == null || componentClass == null || componentClass.isAssignableFrom(item.getClass());
    map.get(key).add(item);
  }

  @Override
  public boolean addAll(final Iterable<IntTo<X>> items) {
    updateMax();
    for (final IntTo<X> item : items) {
      map.put(max++, item);
    }
    return true;
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean addAll(final IntTo<X>... items) {
    updateMax();
    for (final IntTo<X> item : items) {
      map.put(max++, item);
    }
    return true;
  }

  @Override
  public boolean insert(final int pos, final IntTo<X> item) {
    if (pos > max) {
      max = pos+1;
    }
    map.put(pos, item);
    return false;
  }

  @Override
  public boolean contains(final IntTo<X> value) {
    if (value == null) {
      return false;
    }
    main:
    for (final IntTo<X> item : map.values()) {
      if (item.size() == value.size()) {
        for (int key = item.size(); key-->0;) {
          if (!equals(item.get(key), value.get(key))) {
            continue main;
          }
        }
        return true;
      }
    }
    return false;
  }

  private boolean equals(final X x, final X x2) {
    return x == null ? x2 == null : x.equals(x2);
  }

  @Override
  public IntTo<X> at(final int index) {
    return map.get(index);
  }

  @Override
  public int indexOf(final IntTo<X> value) {
    if (value == null) {
      return -1;
    }
    main:
    for (Integer key : map.keys()) {
      final IntTo<X> item = map.get(key);
      if (item.size() == value.size()) {
        for (int j = item.size(); j-->0;) {
          if (!equals(item.get(j), value.get(j))) {
            continue main;
          }
        }
        return key;
      }
    }
    return -1;
  }

  @Override
  public boolean remove(final int index) {
    return map.remove(index) != null;
  }

  @Override
  public boolean findRemove(final IntTo<X> value, final boolean all) {
    if (value == null) {
      return false;
    }
    boolean success = false;
    main:
    for (Integer key : map.keys()) {
      final IntTo<X> item = map.get(key);
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
  public void set(final int index, final IntTo<X> value) {
    if (index > max) {
      max = index+1;
    }
    map.put(index, value);
  }

  @Override
  public void push(final IntTo<X> value) {
    updateMax();
    map.put(max++, value);
  }

  @Override
  public IntTo<X> pop() {
    updateMax();
    max--;
    final IntTo<X> items = map.remove(max);
    if (items != null) {
      return items;
    }
    final Integer[] keys = map.keys().toArray(Integer[]::new);
    Arrays.sort(keys);
    return map.remove(keys[keys.length-1]);
  }

  @Override
  public List<IntTo<X>> asList() {
    final List<IntTo<X>> list = newList();
    final Integer[] keys = map.keys().toArray(Integer[]::new);
    Arrays.sort(keys);
    for (final Integer key : keys) {
      list.add(map.get(key));
    }
    return list;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Set<IntTo<X>> asSet() {
    final Set<IntTo<X>> set = newSet();
    if (map instanceof MapOf) {
      set.addAll(((MapOf) map).values());
    } else {
      for (final IntTo<X> value : map.values()) {
        set.add(value);
      }
    }
    return set;
  }

  @Override
  public Deque<IntTo<X>> asDeque() {
    final Deque<IntTo<X>> deque = newDeque();
    final Integer[] keys = map.keys().toArray(Integer[]::new);
    Arrays.sort(keys);
    for (final Integer key : keys) {
      deque.add(map.get(key));
    }
    return deque;
  }

  protected List<IntTo<X>> newList() {
    return new ArrayList<>();
  }

  protected Set<IntTo<X>> newSet() {
    return new LinkedHashSet<>();
  }

  protected Deque<IntTo<X>> newDeque() {
    return new ArrayDeque<>();
  }

  protected Map<Integer, IntTo<X>> newMap() {
    return new TreeMap<>();
  }

  @Override
  public ObjectTo<Integer, IntTo<X>> clone(final CollectionOptions options) {
    throw new UnsupportedOperationException();
  }

  @Override
  public IntTo<X> put(final Entry<Integer, IntTo<X>> item) {
    return map.put(item);
  }

  @Override
  public Entry<Integer, IntTo<X>> entryFor(final Object key) {
    final int asInt = coerceKey(key);
    return new Entry<Integer, IntTo<X>>() {

      @Override
      public Integer getKey() {
        return asInt;
      }

      @Override
      public IntTo<X> getValue() {
        return map.get(asInt);
      }

      @Override
      public IntTo<X> setValue(final IntTo<X> value) {
        return map.put(asInt, value);
      }
    };
  }

  @Override
  public IntTo<X> get(final Object key) {
    assertValid(key);
    return map.get(coerceKey(key));
  }

  private void assertValid(final Object key) {
    assert coerceKey(key) >= Integer.MIN_VALUE;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void setValue(final Object key, final Object value) {
    Integer k = coerceKey(key);
    assertValid(k);
    if (k > max) {
      max = k;
    }
    map.put(k, (IntTo<X>)value);
  }

  private int coerceKey(Object key) {
    if (key == null) {
      throw new NullPointerException("Null not allowed");
    }
    return key instanceof Number ? ((Number)key).intValue() : Integer.parseInt(String.valueOf(key));
  }

  @Override
  public IntTo<X> remove(final Object key) {
    assertValid(key);
    if (key.equals(max-1)) {
      max--;
    }
    return map.remove(coerceKey(key));
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public IntTo<X>[] toArray() {
    updateMax();
    @SuppressWarnings("unchecked")
    final
    IntTo<X>[] results = new IntTo[max];
    for (int i = max;i-->0;) {
      results[i] = map.get(coerceKey(i));
    }
    assert noNegatives() : "Cannot use .toArray on an IntTo.Many with negative key values: "+map.keys().join("[", ",", "]");
    return results;
  }

  private boolean noNegatives() {
    for (Integer key : map.keys()) {
      if (key < 0) {
        return false;
      }
    }
    return true;
  }

  @Override
  @SuppressWarnings({
      "rawtypes", "unchecked"
  })
  public Collection<IntTo<X>> toCollection(Collection<IntTo<X>> into) {
    if (into == null) {
      into = newList();
    }
    if (map instanceof StringToAbstract) {
      into.addAll(((StringToAbstract) map).valueSet());
    } else {
      final Integer[] keys = map.keys().toArray(Integer[]::new);
      Arrays.sort(keys);
      for (final Integer key : keys) {
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
    for (Entry<Integer, IntTo<X>> e : map.entries()) {
      into.put(e.getKey(), e.getValue());
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


  @Override
  public boolean readWhileTrue(In2Out1<Integer, IntTo<X>, Boolean> callback) {
    if (appearsCompact()) {
      for (int i = 0, m = size(); i < m; i++ ) {
        if (!callback.io(i, get(i))) {
          return false;
        }
      }
    } else {
      final Iterator<Out2<Integer, IntTo<X>>> itr = map.forEachItem()
          .map(e->Out2.out2Immutable(e.out1(), e.out2()))
          .iterator();
      while (itr.hasNext()) {
        final Out2<Integer, IntTo<X>> next = itr.next();
        if (!callback.io(next.out1(), next.out2())) {
          return false;
        }
      }
    }
    return true;
  }

  protected boolean appearsCompact() {
    int s = size();
    if (s == 0) {
      return true;
    }
    return map.containsKey(0) && map.containsKey(s-1);
  }

}
