package xapi.collect.impl;

import java.util.LinkedList;

import xapi.inject.impl.SingletonProvider;

public class LazyLinkedList <T> extends SingletonProvider<LinkedList<T>>{
  @Override
  protected LinkedList<T> initialValue() {
    return new LinkedList<T>();
  }
}
