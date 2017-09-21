/**
 *
 */
package xapi.dev.source;

import xapi.collect.impl.StringStack;
import xapi.fu.Coercible;

/**
 * A lightweight utility to assemble strings using a tree of linked nodes,
 * so you can easily "tear off" a pointer in the buffer, and generate text from multiple locations at once.
 *
 * @author James X. Nelson (james@wetheinter.net, @james)
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class CharBuffer implements Coercible {

  protected static final class CharBufferStack extends StringStack<CharBuffer> {
  }

  volatile StringBuilder target;
  protected String indent = "";
  CharBufferStack head;
  CharBufferStack tail;

  public CharBuffer() {
    this(new StringBuilder());
  }

  public CharBuffer(final String text) {
    this(new StringBuilder(text));
  }

  public CharBuffer(final CharBuffer preamble) {
    this(new StringBuilder());
    head.setValue(preamble);
  }

  public CharBuffer(final StringBuilder target) {
    this.target = target;
    tail = head = new CharBufferStack();
  }

  CharBuffer append(final StringBuilder chars) {
    target.append(chars);
    return this;
  }

  CharBuffer makeChild(){
    final CharBuffer child = newChild();
    child.indent = indent;
    addToEnd(child);
    return child;
  }

  protected CharBuffer newChild() {
    return new CharBuffer();
  }

  protected CharBuffer newChild(final StringBuilder suffix) {
    return new CharBuffer(suffix);
  }

  public void onAppend() { }

  public CharBuffer append(final Object obj) {
    onAppend();
    target.append(coerce(obj));
    return this;
  }

  public CharBuffer append(final String str) {
    onAppend();
    target.append(str);
    return this;
  }

  public CharBuffer append(final char ... chars) {
    onAppend();
    target.append(chars);
    return this;
  }


  public void addToBeginning(final CharBuffer buffer) {
    assert notContained(buffer) : "Infinite recursion!";
    final CharBufferStack newHead = new CharBufferStack();
    newHead.next = head;
    newHead.setValue(buffer);
    head = newHead;
  }

  /**
   * Append the given string, and return a printbuffer to append to this point.
   *
   * @param suffix
   *          - The text to append
   * @return - A buffer pointed at this text, capable of further before/after
   *         branching
   */
  public CharBuffer printAfter(final String suffix) {
    final CharBuffer buffer = newChild(new StringBuilder(suffix));
    addToEnd(buffer);
    return buffer;
  }

  public CharBuffer clear() {
    tail = head = new CharBufferStack();
    target.setLength(0);
    return this;
  }

  public void addToEnd(final CharBuffer buffer) {
    assert notContained(buffer) : "Infinite recursion! On [" + buffer + "] in "
        + this;
    final CharBufferStack newTail = new CharBufferStack();
    newTail.setValue(buffer);
    synchronized (target) { // we synchro on this volatile variable, because we only need the lock if we share a reference.
      final StringBuilder myTarget = target;
      // Cannot convert to method reference; we want to see updates myTarget variables
      //noinspection Convert2MethodRef
      newTail.setPrefix(()->myTarget.toString());
      target = new StringBuilder(); // everyone else will see this change w/out having to synchro
    }
    tail.next = newTail;
    tail = newTail;
  }

  /**
   * Tests to ensure there is no recursion between nodes.
   *
   * Only called when -ea [enable assertions = true]
   */
  private boolean notContained(final CharBuffer buffer) {
    if (buffer == this) {
      System.err.println("Trying to add a buffer to itself");
      return false;
    }
    StringStack<CharBuffer> next = head;
    while (next != null) {
      if (next.getValue() == buffer) {
        System.err.println("Trying to add a buffer that is already a child");
        return false;
      }
      next = next.next;
    }
    next = buffer.head;
    while (next != null) {
      if (next.getValue() == this) {
        System.err.println("Trying to add an ancestor to a child");
        return false;
      }
      next = next.next;
    }
    return true;
  }

  public CharBuffer add(Object ... values) {
    for (Object value : values) {
      append(value);
    }
    return this;
  }

  public CharBuffer ln() {
    append("\n");
    return this;
  }

  @Override
  @Deprecated // prefer .toSource();
  public final String toString() {
    return toSource();
  }

  public String toSource() {
    final StringBuilder body = new StringBuilder();
    body.append(head);
    body.append(target.toString());
    return body.toString();
  }

  public byte[] toBytes() {
    return toSource().getBytes();
  }

  public boolean isEmpty() {
    return toSource().isEmpty();
  }
}
