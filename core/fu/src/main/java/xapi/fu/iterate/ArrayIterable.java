package xapi.fu.iterate;

import xapi.fu.MappedIterable;
import xapi.fu.has.HasSize;

import java.util.Iterator;

public class ArrayIterable <E> implements MappedIterable<E>, HasSize {

  public static <E> ArrayIterable<E> iterate(E ... es) {
    return new ArrayIterable<>(es);
  }

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

  @SuppressWarnings("unchecked") // can't do anything with a zero length array.
  public ArrayIterable(E[] array) {
    this.array = array == null ? (E[])new Object[0] : array;
  }

  @Override
  public Iterator<E> iterator() {
    return new Itr();
  }

  protected void remove(E key) {
    throw new UnsupportedOperationException("ArrayIterable does not support remove");
  }

  @Override
  public int size() {
    return array.length;
  }
}
