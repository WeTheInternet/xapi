package xapi.collect.impl;

import java.util.TreeMap;

import xapi.inject.impl.SingletonProvider;

public class LazyTreeMap <K, V> extends SingletonProvider<TreeMap<K, V>>{
  @Override
  protected TreeMap<K, V> initialValue() {
    return new TreeMap<K, V>();
  }
}