package xapi.collect.impl;

import java.util.Iterator;

/**
 * A very simple, but useful stack (linked list).
 * <p>
 * It's one-way, threadsafe, fast, toString friendly,
 * and can merge with other SimpleStacks easily via {@link #consume(SimpleStack)}
 * <p>
 * Note that neither remove() nor size() are not supported.
 * <p>
 * If you need a list or a map, use one.
 * This class is for pushing together references, iterating through them,
 * and maybe joining them into a string.
 * 
 * @author james@wetheinter.net
 *
 */
public class SimpleStack <T> implements Iterable<T>{

  protected static class Node <T> {
    protected Node<T> next;
    protected T value;
  }

  protected final class NodeIterator implements Iterator<T> {
    private Node<T> next = head.next;
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
  Node<T> tail;
  final Node<T> head = tail = new Node<T>();

  @Override
  public Iterator<T> iterator() {
    return new NodeIterator();
  }

  public T head() {
    return head.next == null ? null : head.next.value;
  }

  public T tail() {
    return tail.value;
  }

  public synchronized void add(T item) {
    Node<T> node = newNode();
    node.value = item;
    tail.next = node;
    tail = node;
  }

  public synchronized void clear() {
    (tail=head).next = null;
  }
  
  /**
   * Forcibly takes all elements from one stack and attaches them to this.
   * <p>
   * This method destroys the stack you send to it.
   * <p>
   * Note that it is threadsafe with respect to the stack consuming,
   * but not so with the stack being consumed (deadlock's no fun).
   * <p>
   * Since you are destroying it anyway, chances are the stack 
   * is getting gc'd as soon as it pops off the, well... [execution] stack!
   * <p>
   * This method is destructive because you _really_ don't want
   * to allow cyclic references when two stacks reference each other.
   * @return 
   */
  public synchronized SimpleStack<T> consume(SimpleStack<T> other) {
    if ((tail.next = other.head.next) != null)
      tail = other.tail;
    (other.tail = other.head).next = null;
    return this;
  }

  public boolean isEmpty() {
    return head == tail;
  }

  /**
   * toStrings the items in the stack with the specified separator
   */
  public String join(String separator) {
    StringBuilder b = new StringBuilder();
    Iterator<T> iter = iterator();
    if (iter.hasNext())
      b.append(iter.next());
    while (iter.hasNext()) {
      b.append(separator).append(iter.next());
    }
    return b.toString();
  }

  protected Node<T> newNode() {
    return new Node<T>();
  }

}
