package xapi.source.lex;

import xapi.source.lex.CharIterator;

public class StringCharIterator implements CharIterator {
  final CharSequence content;
  final int length;
  int current;

  public StringCharIterator(final String content) {
    this.content = content;
    this.length = content.length();
  }

  @Override
  public char next() {
    return content.charAt(current++);
  }

  @Override
  public char peek() {
    return content.charAt(current);
  }

  public CharSequence peekSeq() {
    return content.subSequence(current, current+1);
  }

  @Override
  public boolean hasNext() {
    return current < length;
  }

  @Override
  public String toString() {
    return current == length ? "" : content.subSequence(current, length).toString();
  }

  @Override
  public CharSequence consume(final int size) {
    final int was = current;
    current += size;
    return content.subSequence(was, current);
  }

  @Override
  public CharSequence consumeAll() {
    return consume(content.length()-current);
  }
}
