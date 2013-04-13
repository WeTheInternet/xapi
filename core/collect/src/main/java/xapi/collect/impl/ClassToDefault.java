package xapi.collect.impl;

import java.util.Map;

import xapi.collect.api.ClassTo;
import xapi.collect.proxy.MapOf;

public class ClassToDefault <V> extends MapOf<Class<?>,V> implements ClassTo<V>{

  @SuppressWarnings("unchecked") // Don't worry about casting Class to Class<?>
  public ClassToDefault(Map<Class<?>,V> map, Class<V> valueClass) {
    super(map, Class.class.cast(Class.class), valueClass);
  }



}
