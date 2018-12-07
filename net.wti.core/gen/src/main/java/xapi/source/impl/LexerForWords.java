package xapi.source.impl;

import xapi.source.api.CharIterator;
import xapi.source.api.Lexer;

public class LexerForWords extends LexerDefault {

  @Override
  protected Lexer consume(char c, CharIterator str) {
    switch (c) {
      case '\r':
        if (str.hasNext() && str.peek()=='\n') {
          c = str.next();//treat \r\n as \n
        }
      case ' ':
      case '\n':
      case '\t':
        // TODO add literals for 0xA, and other nbsp values
        return onWhitespace(c, str);
      default:
        // start a word
        return onWordStart(c, str);
    }
  }

  protected boolean isWhitespace(char c) {
    switch (c) {
      case '\r':
      case '\n':
      case '\t':
      case ' ':
        return true;
      default:
        return false;
    }
  }

  protected Lexer onWordStart(char c, CharIterator str) {
    return onWord(extractWord(c, str), str);
  }

  protected String extractWord(char c, CharIterator str) {
    // Consume everything until we see whitespace again
    StringBuilder b = new StringBuilder().append(c);
    while (str.hasNext()) {
      if (isWhitespace(str.peek())) {
        return b.toString();
      }
      b.append(str.next());
    }
    return b.toString();
  }

  protected Lexer onWord(String word, CharIterator str) {
    if (str.hasNext()) {
      return consume(str);
    }
    return this;
  }

  protected Lexer onWhitespace(char c, CharIterator str) {
    // Default implementation does nothing.
    return super.consume(c, str);
  }

}
