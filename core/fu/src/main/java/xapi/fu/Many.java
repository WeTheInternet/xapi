package xapi.fu;

import java.util.Iterator;

/**
 * An extremely simple linked list.
 *
 * All you can do is add and iterate.
 * The most performance use-style is:
 * Many many = new Many(); // keep a reference to the head
 * many.add(1).add(2).add(3); // each add returns the next node to which we want to add to
 *
 * If you don't chain your method calls, each add uses O(n) operations.
 *
 * If you can't chain, you are encouraged to keep a tail pointer around for good measure.
 *
 * Many tail = head = new Many();
 * tail = tail.add(1);
 * tail = tail.add(2);
 * head.forEach(value->{});
 *
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 07/11/15.
 */
public class Many <T> implements Iterable <T> {
  T value;
  Many<T> next;

  Many <T> add(T value) {
    if (next == null) {
      next = new Many<>();
      next.value = value;
      return next;
    } else {
      return next.add(value);
    }
  }

  @Override
  public Iterator<T> iterator() {
    return new Itr<>(this);
  }

  public <R> Many<R> map(In1Out1<T, R> mapper) {
    Many<R> tail, head = tail = new Many<>();
    for (T t : this) {
      final R mapped = mapper.io(t);
      tail = tail.add(mapped);
    }
    return head;
  }

  public <R> Many<R> map(In1Out1<T, R> mapper, int limit) {
    Many<R> tail, head = tail = new Many<>();
    final Iterator<T> itr = iterator();
    T next = null;
    while (limit --> 0) {
      if (itr.hasNext()) {
        next = itr.next();
      }
      final R mapped = mapper.io(next);
      tail = tail.add(mapped);
    }
    return head;
  }

  private static class Itr <T> implements Iterator<T> {
    private Many<T> here;

    public Itr(Many<T> me) {
      here = me;
    }

    @Override
    public boolean hasNext() {
      return here!=null;
    }

    @Override
    public T next() {
      try {
        return here.value;
      }finally {
        here = here.next;
      }
    }
  }

  public void clear() {
    value = null;
    if (next != null) {
      next.clear();
      next = null;
    }
  }
}
