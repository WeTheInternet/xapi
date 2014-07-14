package xapi.source.api;

public interface Lexer {

  Lexer consume(CharIterator str);
  Lexer lex(String str);
  void clear();

}
