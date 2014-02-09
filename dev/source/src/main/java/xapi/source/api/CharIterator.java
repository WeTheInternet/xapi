package xapi.source.api;

/**
 * A simple interface for advancing through a sequence of characters, that
 * communicates that advance back to the source.
 */
public interface CharIterator {
  boolean hasNext();
  char next();

}