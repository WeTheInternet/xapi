package xapi.collect.impl;

import java.util.Iterator;

public class EmptyIterator <E> implements Iterator<E>, Iterable<E>{

  private EmptyIterator() {}
  
  @SuppressWarnings("rawtypes")
  private static final EmptyIterator EMPTY_ITERATOR = new EmptyIterator();
  
  @SuppressWarnings("unchecked")
  public static <E> Iterator<E> getEmptyIterator() {
    return EMPTY_ITERATOR;
  }

  @SuppressWarnings("unchecked")
  public static <E> Iterable<E> getEmptyIterable() {
    return EMPTY_ITERATOR;
  }
  
  @Override
  public boolean hasNext() {
    return false;
  }
  
  @Override
  public Iterator<E> iterator() {
    return this;
  }

  @Override
  public E next() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

}
