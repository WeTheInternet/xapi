package xapi.source.impl;

import xapi.source.api.CharIterator;
import xapi.source.api.Lexer;

public class LexerStack extends LexerForWords {

  private LexerStack next;

  @Override
  protected final Lexer onWord(String word, CharIterator str) {
    if (next != null) {
      return next.onWord(this, word, str);
    }
    return super.onWord(word, str);
  }

  protected Lexer onWord(LexerStack parent, String word, CharIterator str) {
    if (next != null) {
      return next.onWord(parent, word, str);
    }
    return parent.consume(str);
  }

  public LexerStack addLexer(LexerStack consumer) {
    if (next == null) {
      next = consumer;
    } else {
      next.addLexer(consumer);
    }
    return this;
  }

}
