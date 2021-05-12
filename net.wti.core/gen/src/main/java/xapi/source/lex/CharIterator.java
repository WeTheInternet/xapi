package xapi.source.lex;

/**
 * A simple interface for advancing through a sequence of characters, that
 * communicates that advance back to the source.
 */
public interface CharIterator {

  static StringCharIterator forString(String chars) {
    return new StringCharIterator(chars);
  }

  boolean hasNext();
  char next();
  char peek();
  CharSequence consume(int size);
  CharSequence consumeAll();

  default CharSequence readLine() {
    StringBuilder b = new StringBuilder();

    while (hasNext()) {
      char n = next();
      if (n == '\n') {
        return b;
      }
      if (n == '\r') {
        if (peek() == '\n') {
          next();
        }
        return b;
      }
      b.append(n);
    }

    return b;
  }
}
