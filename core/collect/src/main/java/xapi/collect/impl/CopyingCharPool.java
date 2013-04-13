package xapi.collect.impl;

import xapi.collect.api.CharPool;

public class CopyingCharPool implements CharPool {

  @Override
  public char[] getArray(char[] src) {
    char[] copy = new char[src.length];
    System.arraycopy(src, 0, copy, 0, src.length);
    return copy;
  }

  @Override
  public char[] getArray(char[] src, int start, int len) {
    assert start + len <= src.length;
    char[] copy = new char[len];
    System.arraycopy(src, start, copy, 0, len);
    return copy;
  }

  @Override
  public char[] getArray(CharSequence src) {
    int len = src.length();
    char[] copy = new char[len];
    while(len-->0)
      copy[len] = src.charAt(len);
    return copy;
  }

  @Override
  public char[] getArray(CharSequence src, int start, int len) {
    assert start + len <= src.length();
    char[] copy = new char[len];
    while(len-->0)
      copy[len] = src.charAt(start + len);
    return copy;
  }

}