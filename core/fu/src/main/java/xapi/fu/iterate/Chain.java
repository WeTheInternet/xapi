package xapi.fu.iterate;

import xapi.fu.Filter.Filter1;
import xapi.fu.Immutable;
import xapi.fu.In1Out1;
import xapi.fu.MappedIterable;
import xapi.fu.has.HasSize;

import java.util.Iterator;

/**
 * An extremely simple linked list.
 *
 * All you can do is add and iterate.
 * The most performant use-style is:
 * Chain chain = new Chain(); // keep a reference to the head
 * chain.add(1).add(2).add(3); // each add returns the next node to which we want to add to
 *
 * If you don't chain your method calls, each add uses O(n) operations.
 *
 * If you can't chain, you are encouraged to keep a tail pointer around for good measure.
 *
 * Chain tail = head = new Chain();
 * tail = tail.add(1);
 * tail = tail.add(2);
 * head.forEach(value->{});
 *
 * If you need to chain inline and want to keep your head pointer,
 * use the built-in builder helper:
 * Chain itr = new Chain()
 *     .addMany()
 *     .add(1)
 *     .add(2)
 *     .build();
 *
 * Note that we extend GrowableIterable, to allow concatenation of iterables,
 * however, each chain is, itself, just a linked list of nodes.
 *
 * If you concat an iterable onto a chain, then add to the chain,
 * the nodes added to the chain will appear before those added
 * to the appended iterable.
 *
 * This allows you to create a graph-like structure
 * where you can get O(1) insertions in the middle of a linked list,
 * without having to iterate (by storing a pointer to the Chain
 * that you want to insert into).
 *
 * You can also attach the same chain into multiple iterables,
 * to allow reuse of a section of items.
 *
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 07/11/15.
 */
public class Chain<T> implements GrowableIterable<T>, MappedIterable<T> {

  private static class Itr <T> implements Iterator<T> {

    private Chain<T> here;

    public Itr(Chain<T> me) {
      this.here = me;
    }

    @Override
    public boolean hasNext() {
      return here.value!=null;
    }

    @Override
    public T next() {
      try {
        return here.value.out1();
      }finally {
        here = here.next;
      }
    }

  }

  public static <T> ChainBuilder<T> startChain() {
    return new ChainBuilder<>();
  }

  public static <T> ChainBuilder<T> toChain(T ... values) {
    return new ChainBuilder<T>().addAll(values);
  }

  public static class ChainBuilder<T> implements HasSize, MappedIterable<T> {
    private final Chain<T> head;
    private Chain<T> tail, prev;
    private int size;

    public ChainBuilder() {
      this(new Chain<>());
    }
    public ChainBuilder(Chain<T> head) {
      this.head = tail = prev = head;
    }

    public ChainBuilder<T> add(T value) {
      size++;
      prev = tail;
      tail = tail.add(value);
      return this;
    }

    public ChainBuilder<T> addAll(T ... rest) {
      for (T value : rest) {
        size++;
        prev = tail;
        tail = tail.add(value);
      }
      return this;
    }

    public ChainBuilder<T> addAll(Iterable<T> value) {
      for (T v : value) {
        size++;
        prev = tail;
        tail = tail.add(v);
      }
      return this;
    }

    public ChainBuilder<T> addAll(Iterator<T> value) {
      while (value.hasNext()) {
        size++;
        prev = tail;
        tail = tail.add(value.next());
      }
      return this;
    }

    public Chain<T> build() {
      return head;
    }

    @Override
    public int size() {
      return size;
    }

    @Override
    public Iterator<T> iterator() {
      return head.iterator();
    }

    public boolean isEmpty() {
      return head == tail;
    }

    public T last() {
      return prev.value.out1();
    }
  }

  Immutable<T> value;
  Chain<T> next;

  public Chain<T> add(T value) {
    if (next == null) {
      this.value = Immutable.immutable1(value);
      next = new Chain<>();
      return next;
    } else {
      return next.add(value);
    }
  }

  public ChainBuilder<T> addMany() {
    return new ChainBuilder<>(this);
  }

  @Override
  public GrowableIterator<T> iterator() {
    return new GrowableIterator<>(new Itr<>(this));
  }

  public <R> Chain<R> mapImmediate(In1Out1<T, R> mapper) {
    Chain<R> tail, head = tail = new Chain<>();
    for (T t : this) {
      final R mapped = mapper.io(t);
      tail = tail.add(mapped);
    }
    return head;
  }

  public Chain<T> filter(Filter1<T> mapper) {
    Chain<T> tail, head = tail = new Chain<>();
    for (T t : this) {
      if (mapper.filter(t)){
        tail = tail.add(t);
      }
    }
    return head;
  }

  public <R> Chain<R> map(In1Out1<T, R> mapper, int limit) {
    Chain<R> tail, head = tail = new Chain<>();
    final Iterator<T> itr = iterator();
    T next = null;
    if (limit == -1) {
      limit = Integer.MAX_VALUE;
    }
    while (limit --> 0) {
      if (itr.hasNext()) {
        next = itr.next();
      }
      final R mapped = mapper.io(next);
      tail = tail.add(mapped);
    }
    return head;
  }

  public void clear() {
    value = null;
    if (next != null) {
      next.clear();
      next = null;
    }
  }

}
