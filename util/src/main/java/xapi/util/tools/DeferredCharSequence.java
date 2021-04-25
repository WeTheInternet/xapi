package xapi.util.tools;

import xapi.fu.Lazy;
import xapi.fu.Out1;

public class DeferredCharSequence<E> implements CharSequence {

  private final Out1<String> join;

  public DeferredCharSequence(final CharSequence body, final CharSequence chars) {
    join = Lazy.deferred1(()->init(body.toString(), chars.toString()));
  }

  protected String init(String str0, String str1) {
    return str0.concat(str1);
  }

  @Override
  public String toString() {
    return join.out1();
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
