package xapi.collect.impl;

import java.util.Iterator;


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
    Node<T> node = new Node<T>();
    node.value = item;
    tail.next = node;
    tail = node;
  }
  
  public void clear() {
    (tail=head).next = null;
  }

  public boolean isEmpty() {
    return head == tail;
  }

  
  
}
