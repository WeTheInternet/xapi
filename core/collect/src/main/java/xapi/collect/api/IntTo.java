package xapi.collect.api;

import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import xapi.collect.proxy.CollectionProxy;


public interface IntTo <T>
extends CollectionProxy<Integer,T>
{

  static interface Many <T> extends IntTo<IntTo<T>> {
    void add(int key, T item);
  }

  static class IntToIterable <T> implements Iterable <T> {
    private final IntTo<T> self;
    public IntToIterable(IntTo<T> self) {
      this.self = self;
    }
    @Override
    public Iterator<T> iterator() {
      return new IntToIterator<T>(self);
    }
  }
  static class IntToIterator <T> implements Iterator <T> {
    private IntTo<T> source;
    int pos = 0;
    public IntToIterator(IntTo<T> source) {
      this.source = source;
    }
    @Override
    public boolean hasNext() {
      return pos < source.size();
    }
    @Override
    public T next() {
      return source.at(pos++);
    }
    @Override
    public void remove() {
      if (source.remove(pos-1)) {
        pos--;
      }
    }
  }

  Iterable<T> forEach();

  boolean add(T item);

  boolean addAll(Iterable<T> items);

  @SuppressWarnings("unchecked")
  boolean addAll(T ... items);

  boolean insert(int pos, T item);

  boolean contains(T value);

  T at(int index);

  int indexOf(T value);

  boolean remove(int index);

  boolean findRemove(T value, boolean all);

  void set(int index, T value);

  void push(T value);

  T pop();

  /**
   * If this IntTo is mutable,
   * you will be getting a ListProxy, backed by this IntTo.
   * If the underlying IntTo forbids duplicates,
   * the ListProxy will act like a set.
   * <p>
   * If this IntTo is immutable,
   * you are getting an ArrayList you can mutate as you wish.
   */
  List<T> asList();

  /**
   * If this IntTo is mutable,
   * you will be getting a SetProxy, backed by this IntTo.
   *
   * This SetProxy will call remove(item) before every addInternal(item).
   *
   * If this IntTo is immutable,
   * you are getting a HashSet you can mutate as you wish.
   */
  Set<T> asSet();

  /**
   * If this IntTo is mutable,
   * you will be getting a DequeProxy, backed by this IntTo.
   *
   * If the underlying IntTo forbids duplicates,
   * the DequeProxy will act like a set.
   *
   * If this IntTo is immutable,
   * you are getting a LinkedList you can mutate as you wish.
   */
  Deque<T> asDeque();


}
