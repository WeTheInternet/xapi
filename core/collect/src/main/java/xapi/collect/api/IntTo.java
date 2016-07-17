package xapi.collect.api;

import xapi.collect.proxy.CollectionProxy;
import xapi.fu.In1;
import xapi.fu.In1Out1;

import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public interface IntTo <T>
extends CollectionProxy<Integer,T>
{

  interface Many <T> extends IntTo<IntTo<T>> {
    void add(int key, T item);
  }

  class IntToIterable <T> implements Iterable <T> {
    private final IntTo<T> self;
    public IntToIterable(IntTo<T> self) {
      this.self = self;
    }
    @Override
    public Iterator<T> iterator() {
      return new IntToIterator<T>(self);
    }
  }

  class IntToIterator <T> implements Iterator <T> {
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

  default boolean addAll(IntTo<T> items) {
    return addAll(items.toArray());
  }

  @SuppressWarnings("unchecked")
  boolean addAll(T ... items);

  boolean insert(int pos, T item);

  boolean contains(T value);

  T at(int index);

  int indexOf(T value);

  boolean remove(int index);

  default boolean removeValue(T value) {
    return findRemove(value, false);
  }

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

  default boolean forMatches(In1Out1<T, Boolean> matcher, In1<T> callback) {
    boolean matched = false;
    for (T t : forEach()) {
      if (matcher.io(t)) {
        callback.in(t);
      }
    }
    return matched;
  }

  default boolean firstMatch(In1Out1<T, Boolean> matcher, In1<T> callback) {
    for (T t : forEach()) {
      if (matcher.io(t)) {
        callback.in(t);
        return true;
      }
    }
    return false;
  }

  default void removeAll(In1<T> callback) {
    final T[] items = toArray();
    clear();
    for (T item : items) {
      callback.in(item);
    }

  }

  @Override
  default String toString(Integer key, T value) {
    return String.valueOf(value);
  }

}
