package xapi.collect.api;

import xapi.collect.proxy.CollectionProxy;
import xapi.fu.Filter.Filter1;
import xapi.fu.In1;
import xapi.fu.In1Out1;
import xapi.fu.MappedIterable;
import xapi.fu.has.HasItems;

import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public interface IntTo <T>
extends CollectionProxy<Integer,T>, HasItems<T>
{

  interface Many <T> extends IntTo<IntTo<T>> {
    void add(int key, T item);
  }

  class IntToIterable <T> implements MappedIterable <T> {
    private final IntTo<T> self;
    public IntToIterable(IntTo<T> self) {
      this.self = self;
    }
    @Override
    public Iterator<T> iterator() {
      return new IntToIterator<>(self);
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

  @Override
  default MappedIterable<T> forEachItem() {
    return MappedIterable.mapped(forEach());
  }

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

  default boolean removeIf(Filter1<T> value, boolean all) {
    if (all) {
      boolean removed = false;
      // remove backwards, to avoid shifting index issues
      for (int i = size(); i --> 0; ) {
        if (value.filter1(at(i))) {
          remove(i);
          removed = true;
        }
      }
      return removed;
    } else {
      // remove forwards, to remove first instance
      for (int i = 0; i < size(); i++) {
        if (value.filter1(at(i))) {
          remove(i);
          return true;
        }
      }
      return false;
    }
  }

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

  default boolean hasMatch(In1Out1<T, Boolean> matcher) {
    return firstMatch(matcher, In1.ignored());
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

  default boolean removeOne(Filter1<T> callback) {
    return removeIf(callback, false);
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

  default String join(String before, String sep, String after) {
    if (isEmpty()) {
      return "";
    }
    StringBuilder b = new StringBuilder(before);
    final Iterator<T> itr = forEach().iterator();
    if (itr.hasNext()) {
      b.append(itr.next());
    }
    while (itr.hasNext()) {
      b.append(sep)
       .append(itr.next());
    }
    b.append(after);
    return b.toString();
  }
  default String join(String sep) {
    return join("", sep, "");
  }

    static boolean isEmpty(IntTo<?> list) {
      return list == null || list.isEmpty();
    }

    default IntTo<? super T> narrow() {
        return this;
    }
}
