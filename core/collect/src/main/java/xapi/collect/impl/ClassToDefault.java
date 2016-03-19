package xapi.collect.impl;

import xapi.collect.api.ClassTo;
import xapi.collect.proxy.MapOf;

import java.util.Map;

public class ClassToDefault <V> extends MapOf<Class<?>,V> implements ClassTo<V>{

  @SuppressWarnings("unchecked") // Don't worry about casting Class to Class<?>
  public <C extends Class<? extends V>> ClassToDefault(Map<Class<?>,V> map, C valueClass) {
    super(map, Class.class.cast(Class.class), valueClass);
  }



}
