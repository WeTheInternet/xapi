package xapi.source.impl;

import xapi.source.api.CharIterator;
import xapi.source.api.Lexer;

public class LexerDefault implements Lexer {

  @Override
  public final Lexer consume(CharIterator str) {
    if (str.hasNext()) {
      return consume(str.next(), str);
    }
    return this;
  }

  protected Lexer consume(char c, CharIterator str) {
    // default implementation does nothing.
    return consume(str);
  }

  @Override
  public Lexer lex(String in) {
    consume(new StringCharIterator(in));
    return this;
  }

  @Override
  public void clear() {
  }
}
