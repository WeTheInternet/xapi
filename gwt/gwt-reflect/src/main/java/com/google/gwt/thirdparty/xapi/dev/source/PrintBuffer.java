/*
 * Copyright 2013, We The Internet Ltd.
 *
 * All rights reserved.
 *
 * Distributed under a modified BSD License as follow:
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * Redistribution in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution, unless otherwise
 * agreed to in a written document signed by a director of We The Internet Ltd.
 *
 * Neither the name of We The Internet nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package com.google.gwt.thirdparty.xapi.dev.source;

import com.google.gwt.thirdparty.xapi.collect.impl.StringStack;

public class PrintBuffer {

  static final char NEW_LINE = '\n';
  static final String INDENT = "  ";

  protected static final class PrintStack extends StringStack<PrintBuffer> {
  }

  protected static String join(final String sep, final String[] args) {
    if (args.length == 0) {
      return "";
    }
    final StringBuilder b = new StringBuilder(args[0]);
    for (int i = 1, m = args.length; i < m; i++) {
      b.append(sep).append(args[i]);
    }
    return b.toString();
  }

  StringBuilder target;
  protected String indent = "";
  PrintStack head;
  PrintStack tail;
  protected boolean indented = false;

  public PrintBuffer() {
    this(new StringBuilder());
  }

  public PrintBuffer(int indent) {
    this(new StringBuilder());
    while(indent-->0) {
      indent();
    }
  }

  public PrintBuffer(final PrintBuffer preamble) {
    this(new StringBuilder());
    head.setValue(preamble);
  }

  public PrintBuffer(final StringBuilder target) {
    this.target = target;
    tail = head = new PrintStack();
  }

  protected void onAppend() {

  }

  public PrintBuffer append(final Object obj) {
    onAppend();
    target.append(obj);
    return this;
  }

  public PrintBuffer print(final String str) {
    printIndent();
    append(str);
    return this;
  }

  public PrintBuffer append(final String str) {
    onAppend();
    target.append(str);
    return this;
  }

  public PrintBuffer append(final CharSequence s) {
    onAppend();
    target.append(s);
    return this;
  }

  public PrintBuffer append(final CharSequence s, final int start, final int end) {
    onAppend();
    target.append(s, start, end);
    return this;
  }

  public PrintBuffer append(final char[] str) {
    onAppend();
    target.append(str);
    return this;
  }

  public PrintBuffer append(final char[] str, final int offset, final int len) {
    onAppend();
    target.append(str, offset, len);
    return this;
  }

  public PrintBuffer append(final boolean b) {
    onAppend();
    target.append(b);
    return this;
  }

  public PrintBuffer append(final char c) {
    onAppend();
    target.append(c);
    return this;
  }

  public PrintBuffer append(final int i) {
    onAppend();
    target.append(i);
    return this;
  }

  public PrintBuffer append(final long lng) {
    onAppend();
    target.append(lng);
    return this;
  }

  public PrintBuffer append(final float f) {
    onAppend();
    target.append(f);
    return this;
  }

  public PrintBuffer append(final double d) {
    onAppend();
    target.append(d);
    return this;
  }

  public PrintBuffer indent() {
    indent = indent + INDENT;
    return this;
  }

  private void printIndent() {
    if (!indented) {
      target.append(indent);
      indented = true;
    }
  }

  public PrintBuffer indentln(final Object obj) {
    printIndent();
    onAppend();
    target.append(INDENT);
    target.append(obj);
    println();
    return this;
  }

  public PrintBuffer indentln(final String str) {
    printIndent();
    onAppend();
    target.append(INDENT);
    append(str);
    println();
    return this;
  }

  public PrintBuffer indentln(final CharSequence s) {
    printIndent();
    onAppend();
    target.append(INDENT);
    target.append(s);
    println();
    return this;
  }

  public PrintBuffer indentln(final char[] str) {
    printIndent();
    onAppend();
    target.append(INDENT);
    target.append(str);
    println();
    return this;
  }

  public PrintBuffer outdent() {
    final int end = Math.max(0, indent.length() - INDENT.length());
    if (end > 0) {
      indent = indent.substring(0, end);
    } else {
      indent = "";
    }
    return this;
  }

  public PrintBuffer println() {
    onAppend();
    target.append(NEW_LINE);
    indented = false;
    return this;
  }

  public PrintBuffer println(final Object obj) {
    printIndent();
    onAppend();
    target.append(obj);
    println();
    return this;
  }

  public PrintBuffer println(final String str) {
    printIndent();
    onAppend();
    append(str);
    println();
    return this;
  }

  public PrintBuffer println(final CharSequence s) {
    printIndent();
    onAppend();
    target.append(s);
    println();
    return this;
  }

  public PrintBuffer println(final char[] str) {
    printIndent();
    onAppend();
    target.append(str);
    println();
    return this;
  }

  /**
   * Prepend the given string, and return a printbuffer to append to this point.
   *
   * @param prefix
   *          - The text to prepend
   * @return - A buffer pointed at this text, capable of further before/after
   *         branching
   */
  public PrintBuffer printBefore(final String prefix) {
    final PrintBuffer buffer = new PrintBuffer(new StringBuilder(prefix));
    addToBeginning(buffer);
    return buffer;
  }

  public void addToBeginning(final PrintBuffer buffer) {
    assert notContained(buffer) : "Infinite recursion!";
    final PrintStack newHead = new PrintStack();
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
  public PrintBuffer printAfter(final String suffix) {
    final PrintBuffer buffer = new PrintBuffer(new StringBuilder(suffix));
    addToEnd(buffer);
    return buffer;
  }

  public PrintBuffer clear() {
    tail = head = new PrintStack();
    target.setLength(0);
    return this;
  }

  public void addToEnd(final PrintBuffer buffer) {
    assert notContained(buffer) : "Infinite recursion! On [" + buffer + "] in "
        + this;
    final PrintStack newTail = new PrintStack();
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
  private boolean notContained(final PrintBuffer buffer) {
    if (buffer == this) {
      System.err.println("Trying to add a buffer to itself");
      return false;
    }
    StringStack<PrintBuffer> next = head;
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

  protected String header() {
    return "";
  }

  protected String footer() {
    return "";
  }

  protected void setNotIndent() {
    indented = false;
  }

  public PrintBuffer clearIndent() {
    indent = "";
    return this;
  }

  @Override
  public String toString() {
    final StringBuilder body = new StringBuilder(header());
    body.append(head);
    body.append(target.toString());
    return body + footer();
  }

  public boolean isEmpty() {
    return target.length() == 0 && head.next == null;
  }

  public boolean isNotEmpty() {
    return target.length() > 0 || head.next != null;
  }
}
