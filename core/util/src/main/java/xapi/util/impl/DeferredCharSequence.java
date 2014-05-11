package xapi.util.impl;

import javax.inject.Provider;

public class DeferredCharSequence<E> implements CharSequence {

  private final LazyProvider<String> join;

  public DeferredCharSequence(final CharSequence body, final CharSequence chars) {
    join = new LazyProvider<String>(new Provider<String>() {
      @Override
      public String get() {
        return init(body.toString(), chars.toString());
      }
    });
  }

  protected String init(String str0, String str1) {
    return str0.concat(str1);
  }

  @Override
  public String toString() {
    return join.get();
  }

  @Override
  public CharSequence subSequence(int start, int end) {
    return toString().subSequence(start, end);
  }

  @Override
  public int length() {
    return toString().length();
  }

  @Override
  public char charAt(int index) {
    return toString().charAt(index);
  }
}