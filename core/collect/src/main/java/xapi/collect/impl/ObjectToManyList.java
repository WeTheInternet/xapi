package xapi.collect.impl;

import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.collect.api.ObjectTo;
import xapi.collect.proxy.MapOf;
import xapi.util.api.ConvertsValue;

public class ObjectToManyList<K, V> extends MapOf<K, IntTo<V>> implements ObjectTo.Many<K, V>{

  private static final long serialVersionUID = 681636065098625160L;
  private final Class<V> componentClass;

  public ObjectToManyList(final Class<K> keyClass, final Class<V> componentClass, final java.util.Map<K, IntTo<V>> map) {
    super(map, keyClass, Class.class.cast(IntTo.class));
    this.componentClass = componentClass;
  }

  public ObjectToManyList<K, V> add(final String key, final V value) {
    get(key).add(value);
    return this;
  }

  protected IntTo<V> newList() {
    return X_Collect.newList(componentClass);
  }

  @Override
  public IntTo<V> get(Object key) {
    return getOrCompute((K)key, new ConvertsValue<K, IntTo<V>>(){
      @Override
      public IntTo<V> convert(K key) {
        return createList(componentClass);
      }
    });
  }

  protected IntTo<V> createList(Class<V> componentClass) {
    return X_Collect.newList(componentClass);
  }
}
