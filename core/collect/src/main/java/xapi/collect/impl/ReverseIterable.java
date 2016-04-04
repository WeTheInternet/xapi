package xapi.collect.impl;

import java.util.Iterator;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 2/6/16.
 */
public class ReverseIterable <T> implements Iterable<T> {

  private Iterator<T> iterator;
  private Iterable<T> iterable;

  public ReverseIterable(Iterable<T> iterable) {
    this.iterable = iterable;
  }

  public ReverseIterable(Iterator<T> iterator) {
    this.iterator = iterator;
  }

  public static <T> ReverseIterable <T> reverse(Iterable<T> itr) {
    return new ReverseIterable<T>(itr);
  }

  public static <T> ReverseIterable <T> reverse(Iterator<T> itr) {
    return new ReverseIterable<T>(itr);
  }

  @Override
  public Iterator<T> iterator() {
    if (iterable != null) {
      return new ReverseIterator<>(iterable);
    } else {
      return new ReverseIterator<>(iterator);
    }
  }

  public Iterable<T> getIterable() {
    return iterable;
  }

  public Iterator<T> getIterator() {
    return iterator;
  }
}
