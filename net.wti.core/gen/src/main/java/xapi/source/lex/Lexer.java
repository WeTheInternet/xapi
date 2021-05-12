package xapi.source.lex;

public interface Lexer {

  Lexer consume(CharIterator str);
  Lexer lex(String str);
  void clear();

}
