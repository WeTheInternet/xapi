package xapi.fu.iterate;

import xapi.fu.Do;
import xapi.fu.In1;
import xapi.fu.In1.In1Unsafe;
import xapi.fu.In2;
import xapi.fu.In3;
import xapi.fu.MappedIterable;
import xapi.fu.Out2;
import xapi.fu.has.HasSize;
import xapi.fu.iterate.CachingIterator.ReplayableIterable;

import java.util.Iterator;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/28/16.
 */
public class ChainBuilder<T> implements HasSize, MappedIterable<T> {
  private final Chain<T> head;
  private Chain<T> tail, prev;
  private int size;

  public ChainBuilder() {
    this(new Chain<>());
  }

    public ChainBuilder(Chain<T> head) {
    this.head = tail = prev = head;
  }

  public T addReturnValue(T value) {
    add(value);
    return value;
  }

  public ChainBuilder<T> add(T value) {
    size++;
    prev = tail;
    tail = tail.add(value);
    return this;
  }

  public ChainBuilder<T> insert(T value) {
    if (head == tail) {
      prev = tail;
      tail = head.insert(value);
    } else {
      head.insert(value);
    }
    size++;
    return this;
  }

  public Do addUndoable(T value) {
    Out2<Chain<T>, Do> item;
    if (head == tail) {
      prev = tail;
      item = head.addUndoable(value);
      tail = item.out1();
    } else {
      item = head.addUndoable(value);
    }
    size++;
    return item.out2().doAfter(()->size--);
  }

  public Do insertUndoable(T value) {
    Out2<Chain<T>, Do> item;
    if (head == tail) {
      prev = tail;
      item = head.insertUndoable(value);
      tail = item.out1();
    } else {
      item = head.insertUndoable(value);
    }
    size++;
    return item.out2().doAfter(()->size--);
  }

  public ChainBuilder<T> addAll(T... rest) {
    for (T value : rest) {
      size++;
      prev = tail;
      tail = tail.add(value);
    }
    return this;
  }

  public ChainBuilder<T> addMany(Iterable<Chain<T>> values) {
    for (Chain<T> value : values) {
      addAll(value);
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
  public boolean isNotEmpty() {
    return head != tail;
  }

  public T last() {
    return prev.value.out1();
  }

  public void removeAllUnsafe(In1Unsafe<T> callback) {
    removeAll(callback);
  }
  public void removeAll(In1<T> callback) {
    final ReplayableIterable<T> items = cached();
    clear();
    items.forAll(callback);
  }

  public <E> void removeAll(In2<T, E> callback, E extra) {
    final ReplayableIterable<T> items = cached();
    clear();
    items.forAll(callback, extra);
  }

  public <E1, E2> void removeAll(In3<T, E1, E2> callback, E1 extra1, E2 extra2) {
    final ReplayableIterable<T> items = cached();
    clear();
    items.forAll(callback, extra1, extra2);
  }

  public void clear() {
    tail = prev = head;
    head.next = null;
    head.value = null;
    size = 0;
  }

  @Override
  public String toString() {
    return "[ " + join(" , ") + " ]";
  }
}
