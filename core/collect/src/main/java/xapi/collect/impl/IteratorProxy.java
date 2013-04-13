package xapi.collect.impl;

import java.util.Map.Entry;

import javax.inject.Provider;

import xapi.collect.proxy.CollectionProxy;

public class IteratorProxy <K, V> implements Provider<Iterable<Entry<K,V>>> {

  private CollectionProxy<K,V> proxy;

  public IteratorProxy(CollectionProxy<K,V> proxy) {
    this.proxy = proxy;
  }

  @Override
  public Iterable<Entry<K,V>> get() {
    return proxy.toMap(null).entrySet();
  }

}