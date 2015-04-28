package com.google.gwt.thirdparty.xapi.collect.impl;


public class StringStack <T> {
  
  public StringStack<T> next;
  String prefix = "";
  T value;

  public StringStack<T> push(String prefix, T b) {
    StringStack<T> node = new StringStack<T>();
    node.prefix = prefix == null ? "" : prefix;
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
    this.prefix = prefix;
  }
  
  @Override
  public final String toString() {
    return prefix + 
        (next == null ? toString(value) : toString(value) + next);
  }

  protected String toString(T item) {
    return item == null ? "" : item.toString();
  }
  
}
