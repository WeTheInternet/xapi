package xapi.collect.impl;

import java.util.Iterator;

import xapi.except.NotImplemented;

public class ArrayIterable <E> implements Iterable<E> {

  private final E[] array;

  private final class Itr implements Iterator<E> {
    int pos = 0, end = array.length;
    @Override
    public boolean hasNext() {
      return pos < end;
    }
    @Override
    public E next() {
      return array[pos++];
    }
    @Override
    public void remove() {
      ArrayIterable.this.remove(array[pos-1]);
    }
  }

  public ArrayIterable(E[] array) {
    this.array = array;
  }

  @Override
  public Iterator<E> iterator() {
    return new Itr();
  }

  protected void remove(E key) {
    throw new NotImplemented("ArrayIterable does not support remove");
  }

}
