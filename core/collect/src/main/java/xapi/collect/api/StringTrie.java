package xapi.collect.api;

import xapi.util.api.ReceivesValue;

public interface StringTrie<E> {

  interface StringTrieEdge<E> {

    E addValue(CharSequence key, int keyFrom, E value);

    int depth();

    StringTrieCursor<E> hasEdge(CharSequence key, int keyFrom);

    StringTrieEdge<E> highest();

    CharSequence key();

    StringTrieEdge<E> lowest();

    E value();

  }

  interface StringTrieCursor<E> {

    int consumed();

    StringTrieEdge<E> edge();

    CharSequence key();
  }

  // Memory management

  void clear();

  StringTrie<E> compress(CharPool pool);

  void destroy();

  // Getters

  E get(CharSequence key);

  E get(CharSequence key, int start, int len);

  E get(CharSequence key, StringTrieCursor<E> cursor, ReceivesValue<StringTrieCursor<E>> cursorPointer);

  // Iterators

  Iterable<E> findPrefixed(CharSequence key);

  Iterable<E> findPrefixed(CharSequence key, int start, int len);

  // Setters

  E put(CharSequence key, E vaue);

  E put(CharSequence key, int start, int len, E value);

  StringTrieCursor<E> put(CharSequence key, E vaue, StringTrieCursor<E> cursor);

  StringTrieCursor<E> put(CharSequence key, int start, int len, E value, StringTrieCursor<E> cursor);

  // Metadata

  int size();

}
