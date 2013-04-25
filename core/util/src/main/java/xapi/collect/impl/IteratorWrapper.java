package xapi.collect.impl;

import java.util.Iterator;

public class IteratorWrapper <E> implements Iterable<E> {

  private Iterator<E> iter;

  public IteratorWrapper(Iterator<E> iter) {
    this.iter = iter;
  }

  @Override
  public Iterator<E> iterator() {
    return iter;
  }
}
