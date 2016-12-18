package xapi.fu.iterate;

import xapi.fu.MappedIterable;
import xapi.fu.has.HasSize;

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

  public T last() {
    return prev.value.out1();
  }
}
