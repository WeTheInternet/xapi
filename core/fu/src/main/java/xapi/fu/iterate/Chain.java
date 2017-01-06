package xapi.fu.iterate;

import xapi.fu.*;
import xapi.fu.Filter.Filter1;
import xapi.fu.Mutable.MutableAsIO;

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

  public static <T> ChainBuilder<T> startChain() {
    return new ChainBuilder<>();
  }

  public static ChainBuilder<Integer> toChain(int ... values) {
    final ChainBuilder<Integer> b = new ChainBuilder<>();
    for (int value : values) {
      // TODO: use a boxing method that can enable object pooling...
      b.add(new Integer(value));
    }
    return b;
  }
  public static <T> ChainBuilder<T> toChain(T ... values) {
    return new ChainBuilder<T>().addAll(values);
  }

  Immutable<T> value;
  Chain<T> next;
  final MutableAsIO<Chain<T>> tail;

  public Chain() {
    tail = new Mutable<>(this).asIO();
  }

  protected Chain(Chain<T> addTo) {
    next = addTo.next;
    addTo.next = this;
    this.tail = addTo.tail;
    if (next == null) {
      // If we are being added to the only chain w/out a next,
      // then we are the new tail.
      tail.io(this);
    }
  }

  public Chain<T> add(T value) {
    final Chain<T> t = tail.io(null);
//    t.value = Immutable.immutable1(value);
//    Chain<T> newTail = new Chain<>(t);
//    t.next = newTail;
//    tail.in(newTail);
//    return newTail;
    return t.insert(value);
  }

  public Chain<T> insert(T value) {
    final Chain<T> curTail = tail.io(null);
    assert curTail.value == null;
    assert curTail.next == null;

    final Chain<T> myNext = next;
    final Chain<T> nextChain = new Chain<>(this);
    if (myNext == null) {
      tail.io(nextChain);
    }
    nextChain.value = this.value;
    nextChain.next = myNext;
    this.value = Immutable.immutable1(value);
    return nextChain;
  }

  @Override
  public GrowableIterator<T> iterator() {
    return new GrowableIterator<>(Out1.newOut1(()->new ChainIterator<>(Chain.this)));
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
    if (limit < 0) {
      if (limit == -1) {
        limit = Integer.MAX_VALUE;
      } else {
        throw new IndexOutOfBoundsException("Cannot return a negative limit of items; you sent " + limit);
      }
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

    public static <T> Chain<T> append(Chain<T> afters, T of) {
      if (afters == null) {
        return new Chain<T>().add(of);
      }
      return afters.add(of);
    }

  public boolean isEmpty() {
    return value == null;
  }

  public Chain<T> getNext() {
    return next;
  }

  public void setNext(Chain<T> next) {
    if (this == tail.io(null)) {
      tail.io(next);
    }
    this.next = next;
  }
}

class ChainIterator <T> implements Iterator<T> {

  private Chain<T> here;
  private Do remove = ()->{
    throw new IllegalStateException("Cannot call remove() before next()");
  };

  public ChainIterator(Chain<T> me) {
    this.here = me;
  }

  @Override
  public synchronized boolean hasNext() {
    return here != null && here.value!=null;
  }

  @Override
  public synchronized T next() {
    try {
      final Chain<T> mine = here;
      final Chain<T> removable = mine.next;
      remove = () -> {
        mine.setNext(removable.getNext());
        remove = Do.NOTHING;
      };
      return here.value.out1();
    } finally {
      here = here.next;
    }
  }

  @Override
  public synchronized void remove() {
    remove.done();
  }
}
