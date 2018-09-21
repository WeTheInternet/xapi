/**
 *
 */
package xapi.collect.impl;

import xapi.fu.Filter;
import xapi.fu.Filter.Filter1;
import xapi.fu.In1Out1;
import xapi.fu.itr.MappedIterable;
import xapi.fu.X_Fu;

import java.util.Iterator;

/**
 * A simple base class for implementing linked lists. The default implementation
 * is uni-directional (a {@link SimpleStack}), but it leaves room to be extended
 * into a doubly-linked list ({@link SimpleLinkedList}).
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public abstract class AbstractLinkedList<T, N extends AbstractLinkedList.Node<T, N>, L extends AbstractLinkedList<T, N, L>>
  implements MappedIterable<T> {

  protected static class Node<T, N extends Node<T, N>> {
    protected N next;
    protected T value;

    @Override
    public String toString() {
      return "Node [" + value + "]";
    }

  }

  protected final class NodeIterator implements Iterator<T> {
    private N next = head, prev;
    private boolean removed;
    @Override
    public boolean hasNext() {
      return next.next != null;
    }

    @Override
    public T next() {
      if (next.next == null) {
        throw new IllegalStateException();
      }
      try {
        return next.next.value;
      } finally {
        if (removed) {
          removed = false;
        } else {
          prev = next;
        }
        next = next.next;
      }
    }

    @Override
    public void remove() {
      if (removed) {
        throw new IllegalStateException();
      }
      removed = true;
      prev.next = next.next;
      next.next = null;
      next.value = null;
      next = prev;
      if (prev.next == null) {
        tail = prev;
      }
    }

  }
  N tail;
  final N head = tail = newNode(null);

  @SuppressWarnings("unchecked")
  public synchronized L add(final T item) {
    final N node = newNode(item);
    node.value = item;
    tail.next = node;
    onAdd(tail, node);
    tail = node;
    return (L) this;
  }

  public synchronized void clear() {
    (tail = head).next = null;
  }

  /**
   * Forcibly takes all elements from one stack and attaches them to this.
   * <p>
   * This method destroys the stack you send to it.
   * <p>
   * Note that it is threadsafe with respect to the stack consuming, but not so
   * with the stack being consumed (deadlock's no fun).
   * <p>
   * Since you are destroying it anyway, chances are the stack is getting gc'd
   * as soon as it pops off the, well... [execution] stack!
   * <p>
   * This method is destructive because you _really_ don't want to allow cyclic
   * references when two stacks reference each other.
   *
   * @return
   */
  @SuppressWarnings("unchecked")
  public synchronized L consume(
    final L other) {
    onAdd(tail, other.head);
    if ((tail.next = other.head.next) != null) {
      tail = other.tail;
    }
    (other.tail = other.head).next = null;
    return (L) this;
  }

  public T head() {
    return head.next == null ? null : head.next.value;
  }

  public boolean isEmpty() {
    return head == tail;
  }

  @Override
  public Iterator<T> iterator() {
    return new NodeIterator();
  }

  /**
   * toStrings the items in the stack with the specified separator
   */
  public String join(final String separator) {
    final StringBuilder b = new StringBuilder();
    final Iterator<T> iter = iterator();
    if (iter.hasNext()) {
      b.append(iter.next());
    }
    while (iter.hasNext()) {
      b.append(separator).append(iter.next());
    }
    return b.toString();
  }

  public T tail() {
    return tail.value;
  }

  @Override
  public String toString() {
    return getClass().getName() + " [ " + join(", ") + " ]";
  }

  /**
   * Called whenever a node is created, including the {@link #head} node.
   *
   * @param item
   *          -> The item that will become the value for this node. Expect a
   *          null for the head node.
   */
  protected abstract N newNode(T item);

  protected void onAdd(final N previous, final N next) {
  }

  public <R> R findMapped(final Filter1<T> consumer, In1Out1<T, R> mapper) {
    assert consumer != null;
    final Iterator<T> itr = iterator();
    while (itr.hasNext()) {
      final T next = itr.next();
      if (consumer.filter1(next)) {
        return mapper.io(next);
      }
    }
    return null;
  }

  public boolean contains(final T value) {
    return containsMatch(Filter.equalsFilter(value));
  }

  public boolean containsReference(final T value) {
    return containsMatch(Filter.referenceFilter(value));
  }

  public boolean containsMatch(final Filter1<T> consumer) {
    assert consumer != null;
    final Iterator<T> itr = iterator();
    while (itr.hasNext()) {
      final T next = itr.next();
      if (consumer.filter1(next)) {
        return true;
      }
    }
    return false;
  }

  public T find(final Filter1<T> consumer) {
    assert consumer != null;
    final Iterator<T> itr = iterator();
    while (itr.hasNext()) {
      final T next = itr.next();
      if (consumer.filter1(next)) {
        return next;
      }
    }
    return null;
  }

  public boolean remove(T value) {
    return findRemove1(Filter.equalsFilter(value));
  }

  public boolean removeByReference(T value) {
    return findRemove1(Filter.referenceFilter(value));
  }

  public boolean findRemove1(final Filter1<T> matcher) {
    assert matcher != null;
    final Iterator<T> itr = iterator();
    while (itr.hasNext()) {
      final T next = itr.next();
      if (matcher.filter1(next)) {
        itr.remove();
        return true;
      }
    }
    return false;

  }
  public T findNotNull() {
    return find(X_Fu::isNotNull);
  }

  public <R> R findNotNullMapped(In1Out1<T, R> mapper) {
    return findMapped(X_Fu::isNotNull, mapper);
  }

}
