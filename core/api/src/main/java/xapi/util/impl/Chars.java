package xapi.util.impl;

public class Chars implements CharSequence{

  public static final Chars EMPTY_STRING = new Chars(new char[0]);

  public static final class SingleChar implements CharSequence{

    private final char c;

    public SingleChar(char c) {
      this.c = c;
    }

    @Override
    public final int length() {
      return 1;
    }

    @Override
    public char charAt(int index) {
      assert index == 0;
      return c;
    }

    @Override
    public CharSequence subSequence(int start, int end) {
      if (start != 0)
        throw new IllegalArgumentException("SingleChar invalid subSequence "+start+":"+end);
      switch (end) {
      case 1:
        return this;
      case 0:
        return EMPTY_STRING;
      default:
        throw new IllegalArgumentException("SingleChar invalid subSequence "+start+":"+end);
      }
    }

  }

  private final char[] chars;
  final int start;
  private final int length;

  public Chars(char[] chars) {
    this(chars, 0, chars.length);
  }
  public Chars(char[] chars, int start, int end) {
    assert start >= 0;
    assert start <= end;
    assert end <= chars.length;
    this.chars = chars;
    this.start = start;
    this.length = end - start;
  }

  public char[] getChars() {
    return chars;
  }

  @Override
  public int length() {
    return length;
  }

  @Override
  public char charAt(int index) {
    return chars[start + index];
  }

  @Override
  public CharSequence subSequence(int start, int end) {
    if (start == 0 && end == chars.length)
      return this;
    return new Chars(chars, this.start+start, this.start + end);
  }

}
