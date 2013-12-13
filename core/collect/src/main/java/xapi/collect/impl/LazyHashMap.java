package xapi.collect.impl;

import java.util.HashMap;

import xapi.inject.impl.SingletonProvider;

public class LazyHashMap <K, V> extends SingletonProvider<HashMap<K, V>> {

  @Override
  protected HashMap<K, V> initialValue() {
    return new HashMap<K, V>();
  }
}
