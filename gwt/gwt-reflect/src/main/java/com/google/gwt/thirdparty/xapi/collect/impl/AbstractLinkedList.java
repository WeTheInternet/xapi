/**
 *
 */
package com.google.gwt.thirdparty.xapi.collect.impl;

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
  implements Iterable<T> {

  protected static class Node<T, N extends Node<T, N>> {
    protected N next;
    protected T value;

    @Override
    public String toString() {
      return "Node [" + value + "]";
    }

  }

  protected final class NodeIterator implements Iterator<T> {
    private N next = head.next;

    @Override
    public boolean hasNext() {
      return next != null;
    }

    @Override
    public T next() {
      try {
        return next.value;
      } finally {
        next = next.next;
      }
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
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

}
