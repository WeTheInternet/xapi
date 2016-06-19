package xapi.collect.impl;

import xapi.collect.api.CollectionOptions;
import xapi.collect.api.IntTo;
import xapi.collect.api.ObjectTo;
import xapi.collect.proxy.CollectionProxy;
import xapi.fu.In1Out1;
import xapi.fu.In2Out1;
import xapi.fu.Out2;

import javax.inject.Provider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public abstract class ObjectToAbstract<K,V> implements ObjectTo<K,V> {
  public static abstract class ManyAbstract<K,V> extends ObjectToAbstract<K,IntTo<V>> implements ObjectTo.Many<K,V> {

    private Class<V> componentType;
    @SuppressWarnings("unchecked")
    public ManyAbstract(
      Class<K> keyType, Class<V> valueType,
      CollectionProxy<K,IntTo<V>> store,
      Provider<Iterable<Entry<K,IntTo<V>>>> iteratorProvider,
      Comparator<K> keyComparator, Comparator<IntTo<V>> valueComparator) {
      super(keyType, Class.class.cast(IntTo.class),// ya, that's what java generics require...
        store, iteratorProvider, keyComparator, valueComparator);
      this.componentType = valueType;
    }

    @Override
    public IntTo<V> getOrCompute(K key, In1Out1<K, IntTo<V>> factory) {
      IntTo<V> existing = get(key);
      if (existing == null) {
        existing = factory.io(key);
        put(key, existing);
      }
      return existing;
    }

    @Override
    public Class<?> componentType() {
      return componentType;
    }
  }


  private final CollectionProxy<K,V> store;
  private Provider<Iterable<Entry<K,V>>> iterator;
  @SuppressWarnings("rawtypes")
  private Comparator keyComparator;
  @SuppressWarnings("rawtypes")
  private Comparator valueComparator;
  private Class<K> keyType;
  private Class<V> valueType;

  public ObjectToAbstract(
    Class<K> keyType, Class<V> valueType,
    CollectionProxy<K,V> store,
    Provider<Iterable<Entry<K,V>>> iteratorProvider,
    Comparator<K> keyComparator,
    Comparator<V> valueComparator
    ) {
    this.keyType = keyType;
    this.valueType = valueType;
    this.store = store;
    this.iterator = iteratorProvider;
    this.keyComparator = keyComparator;
    this.valueComparator = valueComparator;
  }


  protected Collection<V> newCollection() {
    return new ArrayList<V>();
  }

  protected Map<K,V> newMap() {
    return new HashMap<K, V>();
  }

  /**
   * @return - The class of the type used for keys.
   */
  @Override
  public Class<K> keyType() {
    return keyType;
  }
  /**
   * @return - The class of the type used for values.<br>
   * Note that this may not be the value type V; {@link ObjectTo.Many} uses IntTo<V>
   */
  @Override
  public Class<V> valueType() {
    return valueType;
  }
  /**
   * @return - The class of the root component type of values.
   * Where {@link ObjectTo.Many#valueType()} returns IntTo<V>.class,
   * getComponentType() will return V.class
   */
  @Override
  public Class<?> componentType() {
    return valueType;
  }

  @Override
  public Iterable<Entry<K,V>> entries() {
    return iterator.get();
  }

  @Override
  public V[] toArray() {
    return store.toArray();
  }

  @Override
  public Collection<V> toCollection(Collection<V> into) {
    if (into == null) {
      into = newCollection();
    }
    fillCollection(into);
    return into;
  }


  protected void fillCollection(Collection<V> into) {
    for (Entry<K, V> key : entries()) {
      into.add(key.getValue());
    }
  }

  protected void fillMap(Map<K,V> into) {
    for (Entry<K, V> key : entries()) {
      into.put(key.getKey(), key.getValue());
    }
  }

  @Override
  public Map<K,V> toMap(Map<K,V> into) {
    if (into == null) {
      into = newMap();
    }
    fillMap(into);
    return into;
  }

  @Override
  public ObjectTo<K,V> clone(CollectionOptions options) {
    ObjectTo<K,V> map = null;
    return map;
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean containsKey(Object key) {
    for (Entry<K,V> entry : entries()) {
      if (keyComparator.compare(entry.getKey(), key) == 0) return true;
    }
    return false;
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean containsValue(Object value) {
    for (Entry<K,V> entry : entries()) {
      if (valueComparator.compare(entry.getValue(), value) == 0) return true;
    }
    return false;
  }

  @Override
  public V get(Object key) {
    return store.get(key);
  }

  @Override
  public V put(Entry<K, V> item) {
    return store.put(item);
  }

  @Override
  public V remove(Object key) {
    return store.remove(key);
  }

  @Override
  public void putAll(Iterable<Entry<K,V>> items) {
    for (Entry<K,V> item : items) {
      assert item != null;
      put(item.getKey(), item.getValue());
    }
  }

  @Override
  public void addAll(Iterable<Out2<K, V>> items) {
    for (Out2<K,V> item : items) {
      assert item != null;
      put(item.out1(), item.out2());
    }
  }

  @Override
  public void removeAll(Iterable<K> items) {
    for (K key : items) {
      remove(key);
    }
  }

  @Override
  public Iterable<K> keys() {
    return new EntryKeyAdapter<K, V>(entries());
  }

  @Override
  public Iterable<V> values() {
    return new EntryValueAdapter<K, V>(entries());
  }

  @Override
  public int size() {
    return store.size();
  }

  @Override
  public void clear() {
    store.clear();
  }

  @Override
  public boolean isEmpty() {
    return store.isEmpty();
  }


  @Override
  public boolean readWhileTrue(In2Out1<K, V, Boolean> callback) {
    for (Entry<K, V> e : entries()) {
      if (!callback.io(e.getKey(), e.getValue())) {
        return false;
      }
    }
    return true;
  }

}
