package xapi.collect.simple;

import xapi.fu.Out1;

import static xapi.fu.Immutable.immutable1;

public class StringStack <T> {

  private static final Out1<String> EMPTY_STRING = immutable1("");
  public StringStack<T> next;
  Out1<String> prefix;
  T value;

  public StringStack() {
    prefix = EMPTY_STRING;
  }

  public StringStack<T> push(String prefix, T b) {
    StringStack<T> node = new StringStack<T>();
    if (prefix != null) {
      node.prefix = immutable1(prefix);
    } else {
      node.prefix = EMPTY_STRING;
    }
    node.value = b;
    assert next == null : "Pushing to the same stack twice overwrites old value.";
    // Instead, just do tail = tail.push("", value);
    next = node;
    return node;
  }


  public T getValue() {
    return value;
  }

  public void setValue(T value) {
    this.value = value;
  }

  public void setPrefix(String prefix) {
    this.prefix = immutable1(prefix);
  }

  public void setPrefix(Out1<String> prefix) {
    this.prefix = prefix == null ? EMPTY_STRING : prefix;
  }

  @Override
  public final String toString() {
    return prefix.out1() +
        (next == null ? toString(value) : toString(value) + next);
  }

  protected String toString(T item) {
    return item == null ? "" : item.toString();
  }

}
