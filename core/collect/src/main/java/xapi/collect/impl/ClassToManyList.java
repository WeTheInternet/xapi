package xapi.collect.impl;

import xapi.collect.api.ClassTo;
import xapi.collect.api.IntTo;

public class ClassToManyList<K, V> extends ObjectToManyList<Class<?>, V> implements ClassTo.Many<V>{

  private static final long serialVersionUID = 681636065098625160L;

  public ClassToManyList(final Class<V> componentClass, final java.util.Map<Class<?>, IntTo<V>> map) {
    super(Class.class.cast(Class.class), componentClass, map);
  }

}
