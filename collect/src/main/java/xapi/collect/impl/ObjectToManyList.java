package xapi.collect.impl;

import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.collect.api.ObjectTo;
import xapi.collect.proxy.impl.MapOf;

public class ObjectToManyList<K, V> extends MapOf<K, IntTo<V>> implements ObjectTo.Many<K, V>{

  private static final long serialVersionUID = 681636065098625160L;
  private final Class<V> componentClass;

  public <GenericV extends V> ObjectToManyList(final Class<K> keyClass, final Class<GenericV> componentClass, final java.util.Map<K, IntTo<V>> map) {
    super(map, keyClass, Class.class.cast(IntTo.class));
    Class forget = componentClass;
    this.componentClass = forget;
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
    IntTo<V> was = super.get(key);
    if (was == null) {
      was = createList(componentClass);
      put((K) key, was);
    }
    return was;
  }

  protected IntTo<V> createList(Class<V> componentClass) {
    return X_Collect.newList(componentClass);
  }
}
