package xapi.fu.iterate;

import xapi.fu.MappedIterable;

import java.util.Iterator;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 2/6/16.
 */
public class ReverseIterable <T> implements MappedIterable<T> {

  private Iterator<T> iterator;
  private Iterable<T> iterable;

  public ReverseIterable(Iterable<T> iterable) {
    this.iterable = iterable;
  }

  public ReverseIterable(Iterator<T> iterator) {
    this.iterator = iterator;
  }

  public static <T> ReverseIterable <T> reverse(Iterable<T> itr) {
    return new ReverseIterable<>(itr);
  }

  public static <T> ReverseIterable <T> reverse(Iterator<T> itr) {
    return new ReverseIterable<>(itr);
  }

  @Override
  public Iterator<T> iterator() {
    if (iterable != null) {
      return new ReverseIterator<>(iterable);
    } else {
      return new ReverseIterator<>(iterator);
    }
  }

  @Override
  public MappedIterable<T> reversed() {
    return iterable == null
        ? MappedIterable.mappedOneShot(iterator)
        : MappedIterable.mapped(iterable);
  }

  public Iterable<T> getIterable() {
    return iterable;
  }

  public Iterator<T> getIterator() {
    return iterator;
  }

  @SuppressWarnings("unchecked")
  public static <T> MappedIterable<T> reversed(Iterable src) {
    if (src instanceof ReverseIterable) {
      // Only get to short-circuit when class is declared reversible.
      // TODO: consider IsReversible interface instead of instanceof on a class
      return ((MappedIterable<T>)src).reversed();
    }
    // All other iterable get wrapped.
    return new ReverseIterable<>(src);
  }
}
