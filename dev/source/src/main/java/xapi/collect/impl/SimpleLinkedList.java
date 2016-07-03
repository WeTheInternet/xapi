/**
 *
 */
package xapi.collect.impl;

import xapi.fu.Filter.Filter1;
import xapi.fu.In1;
import xapi.fu.In1Out1;
import xapi.fu.X_Fu;

import java.util.Iterator;
import java.util.ListIterator;

/**
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public class SimpleLinkedList<T>
  extends
AbstractLinkedList<T, SimpleLinkedList.LinkedListNode<T>, SimpleLinkedList<T>> {

  protected static class LinkedListNode<T> extends
    AbstractLinkedList.Node<T, LinkedListNode<T>> {
    LinkedListNode<T> previous;
  }

  protected final class ListIter implements ListIterator<T> {

    private LinkedListNode<T> node;
    int pos;

    public ListIter(final LinkedListNode<T> start, final int pos) {
      this.node = start;
      this.pos = pos;
    }

    @Override
    public void add(final T e) {
      final LinkedListNode<T> newNode = newNode(e);
      newNode.value = e;
      newNode.next = node.next;
      newNode.previous = node;
      if (node.next == null) {
        tail = newNode;
      } else {
        node.next.previous = newNode;
      }
      node.next = newNode;
      onAdd(node, newNode);
      node = newNode;
    }

    @Override
    public boolean hasNext() {
      return node != null && node.next != null;
    }

    @Override
    public boolean hasPrevious() {
      return node == null ? tail != null : node.previous != null;
    }

    @Override
    public T next() {
      try {
        return node.next.value;
      } finally {
        pos++;
        node = node.next;
      }
    }

    @Override
    public int nextIndex() {
      return pos + 1;
    }

    @Override
    public T previous() {
      try {
        return node == null ? tail.value : node.value;
      } finally {
        pos--;
        node = node == null ? tail.previous : node.previous == null ? head
          : node.previous;
      }
    }

    @Override
    public int previousIndex() {
      return pos - 1;
    }

    @Override
    public void remove() {
      final LinkedListNode<T> next = node.next;
      node.next = next.next;
      if (next.next == null) {
        tail = node;
      } else {
        next.next.previous = node;
      }
      node = next;
    }

    @Override
    public void set(final T e) {
      node.value = e;
    }

  }


  public void forEachReverse(final In1<T> consumer) {
    assert consumer != null;
    final Iterator<T> reverse = iteratorReverse();
    while (reverse.hasNext()) {
      consumer.in(reverse.next());
    }
  }

  public T findReverse(final Filter1<T> consumer) {
    assert consumer != null;
    final Iterator<T> reverse = iteratorReverse();
    while (reverse.hasNext()) {
      final T next = reverse.next();
      if (consumer.filter1(next)) {
        return next;
      }
    }
    return null;
  }

  public <R> R findMappedReverse(final Filter1<T> consumer, In1Out1<T, R> mapper) {
    assert consumer != null;
    final Iterator<T> reverse = iteratorReverse();
    while (reverse.hasNext()) {
      final T next = reverse.next();
      if (consumer.filter1(next)) {
        return mapper.io(next);
      }
    }
    return null;
  }

  public T findNotNullReverse() {
    return findReverse(X_Fu::returnNotNull);
  }

  public <R> R findNotNullMappedReverse(In1Out1<T, R> mapper) {
    return findMappedReverse(X_Fu::returnNotNull, mapper);
  }

  public Iterator<T> iteratorReverse() {
    return new NodeIteratorReverse();
  }

  public Iterable<T> reverse() {
    return NodeIteratorReverse::new;
  }

  protected final class NodeIteratorReverse implements Iterator<T> {
    private LinkedListNode<T> next = tail;

    @Override
    public boolean hasNext() {
      return next != head;
    }

    @Override
    public T next() {
      try {
        return next.value;
      } finally {
        next = next.previous;
      }
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

  }

  public ListIterator<T> listIterator() {
    return new ListIter(head, 0);
  }

  @Override
  protected LinkedListNode<T> newNode(final T item) {
    return new LinkedListNode<T>();
  }

  @Override
  protected void onAdd(final LinkedListNode<T> previous,
    final LinkedListNode<T> next) {
    next.previous = previous;
  }

  public synchronized T pop() {
    if (head == tail) {
      throw new IllegalStateException("Cannot pop from an empty list");
    }
    T value = tail.value;
    tail.value = null;
    tail = tail.previous;
    return value;
  }
}
