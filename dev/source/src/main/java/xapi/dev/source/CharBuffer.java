/**
 *
 */
package xapi.dev.source;

import xapi.collect.impl.StringStack;

/**
 * A lightweight utility to assemble strings wit
 * @author James X. Nelson (james@wetheinter.net, @james)
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class CharBuffer {

  protected static final class CharBufferStack extends StringStack<CharBuffer> {
  }

  StringBuilder target;
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

  CharBuffer next;

  CharBuffer append(final StringBuilder chars) {
    return this;
  }

  CharBuffer makeChild(){
    final CharBuffer child = newChild();
    if (next != null) {
      next.next = child;
    }
    next = child;
    return child;
  }

  protected CharBuffer newChild() {
    return new CharBuffer();
  }

  protected CharBuffer newChild(final StringBuilder from) {
    return new CharBuffer(from);
  }

  protected void onAppend() {
  }

  public CharBuffer append(final Object obj) {
    onAppend();
    target.append(obj);
    return this;
  }

  public CharBuffer append(final String str) {
    onAppend();
    target.append(str);
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
    newTail.setPrefix(target.toString());
    target.setLength(0);
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


  @Override
  public String toString() {
    final StringBuilder body = new StringBuilder();
    body.append(head);
    body.append(target.toString());
    return body.toString();
  }

}
