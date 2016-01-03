package xapi.gen;

import xapi.fu.Out1;
import xapi.gen.NodeWithParentWithChildren.ChildStack;

import static xapi.fu.Out1.immutable1;

import java.util.Iterator;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 12/12/15.
 */
public abstract class NodeWithParentWithChildren
    <
        Parent extends GenBuffer <?, Parent>,
        Self extends GenBuffer <Parent, Self>,
        Child extends GenBuffer<Self, Child>,
        Stack extends ChildStack<Child>
    > extends NodeWithParent<Parent, Self> {

  protected static class ChildStack <V> {

    private ChildStack next;
    private final Out1<V> value;

    public static <V> ChildStack<V> of(V value) {
      return new ChildStack<>(immutable1(value));
    }

    public ChildStack(Out1<V> value) {
      this.value = value;
    }

    public void setNext(ChildStack newTail) {
      next = newTail;
    }

    public ChildStack getNext() {
      return next;
    }

    public boolean hasNext() {
      return next != null;
    }

    public V getValue() {
      return value.out1();
    }
  }

  protected class ChildIterator implements Iterator<Child> {

    ChildStack<Child> start = head.out1();

    @Override
    public boolean hasNext() {
      return start.hasNext();
    }

    @Override
    public Child next() {
      start = start.getNext();
      return start.getValue();
    }
  }

  protected Out1<Stack> head;
  protected volatile ChildStack tail;


  public NodeWithParentWithChildren() {

  }

  protected NodeWithParentWithChildren(Self node) {
    this();
    this.node = node;
  }

  public final synchronized void addChild(Child child) {
    final ChildStack newTail = newStack(child);
    tail.setNext(newTail);
    tail = newTail;
    onChildAdded(child);
  }

  protected void onChildAdded(Child child) {
    // intentionally empty
  }

  protected abstract Stack newStack(Child child);

  public Iterable<Child> children() {
    return ChildIterator::new;
  }

}
