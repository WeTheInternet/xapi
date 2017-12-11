package xapi.fu.iterate;

import xapi.fu.Out1;

import java.util.Iterator;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 2/6/16.
 */
public class ReverseIterator <T> implements Iterator<T> {

  private static final Out1 END = () -> {
    assert false : "You should never attempt to get a value from the end provider";
    return null;
  };

  private Out1<T> supplier;
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

  private Out1<T> consume(final Out1<T> before, Iterator<T> forward, int count) {
    // yes, we are using recursion to form our stack (using call stack instead of stack object).
    assert count < 1024 * 1024 * 2 : "You are using a ReverseIterator on an iterable containing 2^21 objects; this iterator, " + forward + " either never runs out, " +
        "or you are using very large collections recklessly.";
    if (forward.hasNext()) {
      final T value = forward.next();
      final Out1<T> here = () ->{
        supplier = before;
        return value;
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
    return supplier.out1();
  }

  public Iterator<T> getOriginal() {
    return original;
  }

  public Iterable<T> getIterable() {
    return iterable;
  }
}
