package xapi.source.impl;

import xapi.source.api.CharIterator;

public class StringCharIterator implements CharIterator {
  final String content;
  final int length;
  int current = 0;

  public StringCharIterator(String content) {
    this.content = content;
    this.length = content.length();
  }

  @Override
  public char next() {
    return content.charAt(current++);
  }

  public char peek() {
    return content.charAt(current);
  }
  
  public String peekString() {
    return content.substring(current, current+1);
  }

  @Override
  public boolean hasNext() {
    return current < length;
  }
  
  @Override
  public String toString() {
    return current == length ? "" : content.substring(current);
  }
}