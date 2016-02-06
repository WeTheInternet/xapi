package xapi.collect.impl;

import xapi.util.api.ProvidesValue;

import java.util.Iterator;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 2/6/16.
 */
public class ReverseIterator <T> implements Iterator<T> {

  private static final ProvidesValue END/* http://goo.gl/W2Slg6 */ = new ProvidesValue() {
    @Override
    public Object get() {
      assert false : "You should never attempt to get a value from the end provider";
      return null;
    }
  };

  private T next;
  private ProvidesValue<T> supplier;
  private Iterable<T> iterable;
  private final Iterator<T> original;

  public ReverseIterator(Iterator<T> forward) {
    this.original = forward;
    supplier = consume(END, forward, 0);
  }

  public ReverseIterator(Iterable<T> forward) {
    this(forward.iterator());
    this.iterable = forward;
  }

  private ProvidesValue<T> consume(final ProvidesValue<T> before, Iterator<T> forward, int count) {
    // yes, we are using recursion to form our stack (using call stack instead of stack object).
    assert count < 1024 * 1024 * 2 : "You are using a ReverseIterator on an iterable containing 2^21 objects; this iterator, " + forward + " either never runs out, " +
        "or you are using very large collections recklessly.";
    if (forward.hasNext()) {
      final T value = forward.next();
      final ProvidesValue<T> here = new ProvidesValue<T>() {
        @Override
        public T get() {
          supplier = before;
          return value;
        }
      };
      return consume(here, forward, count+1); // will return the deepest node
    } else {
      // terminal; this deepest node should be the first provided value.
      // our before value is the last item in the iterator, and will be the first one we supply
      return before;
    }
  }

  @Override
  public boolean hasNext() {
    return supplier != END;
  }

  @Override
  public T next() {
    return supplier.get();
  }

  public Iterator<T> getOriginal() {
    return original;
  }

  public Iterable<T> getIterable() {
    return iterable;
  }
}
