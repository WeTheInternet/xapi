package xapi.collect.impl;

import xapi.fu.Lazy;

import java.util.HashMap;

public class LazyHashMap <K, V> extends Lazy<HashMap<K, V>> {

  public LazyHashMap() {
    super(HashMap::new);
  }
}
