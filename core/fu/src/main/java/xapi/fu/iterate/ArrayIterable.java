package xapi.fu.iterate;

public class ArrayIterable <E> implements SizedIterable <E> {

  private final int start;
  private final int end;

  public static <E> ArrayIterable<E> iterate(E ... es) {
    return new ArrayIterable<>(es);
  }

  private final E[] array;

  private final class Itr implements SizedIterator<E> {
    int pos = start, finish = end;
    @Override
    public boolean hasNext() {
      return pos < finish;
    }
    @Override
    public E next() {
      return array[pos++];
    }
    @Override
    public void remove() {
      ArrayIterable.this.remove(array[pos-1]);
    }

    @Override
    public int size() { // size of the iterator is whatever is left. iterable == full size.
      return finish - pos;
    }
  }

  private final class ItrReverse implements SizedIterator<E> {
    int pos = end, finish = start;
    @Override
    public boolean hasNext() {
      return pos > finish;
    }
    @Override
    public E next() {
      return array[--pos];
    }
    @Override
    public void remove() {
      ArrayIterable.this.remove(array[pos+1]);
    }

    @Override
    public int size() { // size of the iterator is whatever is left. iterable == full size.
      return pos - finish;
    }
  }

  public ArrayIterable(E[] array) {
    this(array, 0, array == null ? 0 : array.length);
  }

  @SuppressWarnings("unchecked") // can't do anything with a zero length array.
  public ArrayIterable(E[] array, int start, int end) {
    this.array = array == null ? (E[])new Object[0] : array;
    this.start = start;
    this.end = end;
    assert boundsCheck();
  }

  private boolean boundsCheck() {
    assert start >= 0 : "Invalid (start can't be negative) " + start;
    assert end >= 0 : "Invalid (end can't be negative) " + end;
    assert array == null || end <= array.length : "Invalid; end (" + end + ") must be less than array.length (" + array.length+")";
    return true;
  }

  @Override
  public SizedIterator<E> iterator() {
    return new Itr();
  }

  public SizedIterator<E> iteratorReverse() {
    return new ItrReverse();
  }

  protected void remove(E key) {
    throw new UnsupportedOperationException("ArrayIterable does not support remove");
  }

  @Override
  public int size() {
    return end - start;
  }

  @Override
  public String toString() {
    StringBuilder b = new StringBuilder("[");
    if (array.length > 0) {
      // iterate all but last element
      for (int i = 0, m = array.length - 1; i < m; i++ ) {
        b.append(array[i]);
        b.append(", ");
      }
      // pop on the end item
      b.append(array[array.length-1]);// already checked zero length
    }
    return b.append("]").toString();
  }
}
