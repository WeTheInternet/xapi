package xapi.fu.iterate;

import xapi.fu.MappedIterable;

import java.util.Iterator;

public class SingletonIterator <X> implements MappedIterable<X> {

  private final class Iter implements Iterator<X> {
    private X object;

    public Iter(X object) {
      this.object = object;
    }

    @Override
    public boolean hasNext() {
      return object != null;
    }

    @Override
    public X next() {
      try {
        return object;
      } finally {
        object = null;
      }
    }

    @Override
    public void remove() {
      object = null;
    }
  }

  private final X singleton;

  public SingletonIterator(X singleton) {
    this.singleton = singleton;
  }

  public X getItem() {
    return singleton;
  }

  @Override
  public Iterator<X> iterator() {
    return new Iter(singleton);
  }

  public static <T> SingletonIterator<T> singleItem(T item) {
    return new SingletonIterator<>(item);
  }
}
