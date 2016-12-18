package xapi.collect.impl;

import xapi.annotation.inject.InstanceDefault;
import xapi.collect.api.Fifo;
import xapi.util.api.ConvertsValue;

import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A simple, fast, threadsafe, one-way, single-linked list.
 *
 * This primitive collection eats nulls (ignores them on add),
 * it will only return null from take() when it is empty,
 * and it handles concurrency by synchronizing on a single object for the
 * whole collection.  A read in the middle of a write might get missed if
 * the timing is very close, but no reads will be missed it you call take()
 * until the fifo is drained.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 * @param <E> - The type of item stores in the fifo
 */
@InstanceDefault(implFor=Fifo.class)
public class SimpleFifo <E> implements Fifo<E>, Iterable<E>, Serializable{

  private static final long serialVersionUID = 2525421548842680240L;
  private class Itr implements Iterator<E> {

    Node<E> last = head, node = last;

    @Override
    public boolean hasNext() {
      return node.next != null;
    }

    @Override
    public E next() {
      last = node;
      node = node.next;
      if (node == null)
        throw new NoSuchElementException();
      return node.item;
    }

    @Override
    public void remove() {
      Node<E> expected = node;
      synchronized(head) {
        if (last.next != expected)
          return;
        if (node == tail)
          tail = last;
        if (last == node) {
          if (last == head)
            throw new IllegalStateException();
          return;
        }
        last.next = node.next;
        node.next = null;//help gc
        node = last;
        size--;
      }
    }
  }

  @Override
  public boolean contains(E item) {
    for (E e : forEach())
      if (equals(item,e))
        return true;
    return false;
  }

  protected final static class Node <E> implements Serializable {

    public Node() {

    }

    private static final long serialVersionUID = -4821223918445216546L;
    public E item;
    public Node<E> next;
    @Override
    public String toString() {
      return String.valueOf(item);
    }
  }

  protected Node<E> head = new Node<>();
  private transient Node<E> tail;
  private int size;

  public SimpleFifo() {
    tail = head;
  }

  public SimpleFifo(E[] els) {
    this();
    for (E el : els) {
      if (el == null) {
        return;
      }
      give(el);
    }
  }

  public SimpleFifo(Iterable<E> els) {
    this();
    for (E el : els) {
      if (el != null) {
        give(el);
      }
    }
  }

  @Override
  public Fifo<E> give(E item) {
    if (item==null)return this;
    Node<E> add = new Node<>();
    add.item = item;
    synchronized (head) {
      size++;
      tail.next = add;
      tail = add;
    }
    return this;
  }

  @Override
  public void clear() {
    head.next = null;
    tail = head;
    size = 0;
  }

  @Override
  public boolean remove(E item) {
    Node<E> start = head, next = head.next;
    int was;
    synchronized(head) {
      was = size;
      while (next != null) {
        if (equals(next.item, item)) {
          size--;
          if (next == tail) {
            tail = start;
            tail.next = null;
          } else {
            start.next = next.next;
          }
          next.next = null;//help gc
          next = start.next;
        }else {
          start = next;
          next = next.next;
        }
      }
    }
    return was>size;
  }

  protected boolean equals(E one, E two) {
    return one.equals(two);
  }

  @Override
  public E take() {
    synchronized (head) {
      Node<E> next = head.next;
      if (next == null) {
        tail = head;
        return null;
      }
      size--;
      head.next = next.next;
      if (next == tail)
        tail = head;
      try {
        return next.item;
      }finally {
        //drop all unneeded references!
        next.item = null;
      }
    }
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public boolean isEmpty() {
    return head == tail;
  }

  @Override
  protected void finalize() throws Throwable {
    synchronized (head) {
      head.next = null;
      tail = null;
    }
  }


  @Override
  public String toString() {
    return " ["+head.next +" -> "+tail+"]";
  }

  @Override
  public Iterator<E> iterator() {
    return new Itr();
  }
  @Override
  public Iterable<E> forEach() {
    return this;
  }

  @Override
  public String join(String delim) {
    Node<E> n = head.next;
    if (n == null)return "";
    StringBuilder b = new StringBuilder();
    b.append(n.item);
    while ((n = n.next)!=null) {
      b.append(delim);
      b.append(n.item);
    }
    return b.toString();
  }

  public String join(String delim, ConvertsValue<E, String> serializer) {
    Node<E> n = head.next;
    if (n == null)return "";
    StringBuilder b = new StringBuilder();
    b.append(serialize(serializer, n.item));
    while ((n = n.next)!=null) {
      b.append(delim);
      b.append(serializer.convert(n.item));
    }
    return b.toString();
  }

  protected String serialize(ConvertsValue<E, String> serializer, E item) {
    return serializer == null ? String.valueOf(item) : serializer.convert(item);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Fifo<E> giveAll(E ... elements) {
    for (E element : elements)
      give(element);
    return this;
  }
  @Override
  public Fifo<E> giveAll(Iterable<E> elements) {
    for (E element : elements)
      give(element);
    return this;
  }

}
