package xapi.collect.impl;

import xapi.fu.Lazy;

import java.util.TreeMap;

public class LazyTreeMap <K, V> extends Lazy<TreeMap<K, V>> {

  public LazyTreeMap() {
    super(TreeMap::new);
  }
}
