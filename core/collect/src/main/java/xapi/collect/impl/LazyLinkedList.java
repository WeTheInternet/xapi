package xapi.collect.impl;

import xapi.fu.Lazy;

import java.util.LinkedList;

public class LazyLinkedList <T> extends Lazy<LinkedList<T>> {

  public LazyLinkedList() {
    super(LinkedList::new);
  }
}
