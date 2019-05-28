package xapi.collect.trie;

import xapi.collect.api.CharPool;
import xapi.collect.api.StringTrie;
import xapi.util.api.ReceivesValue;

public class StringTrieAbstract <E> implements StringTrie<E>{

  static class AbstractTrieCursor <E> implements StringTrieCursor<E> {

    @Override
    public int consumed() {
      return 0;
    }

    @Override
    public StringTrieEdge<E> edge() {
      return null;
    }

    @Override
    public CharSequence key() {
      return null;
    }

  }

  @Override
  public void clear() {

  }

  @Override
  public StringTrie<E> compress(CharPool pool) {
    return this;
  }

  @Override
  public void destroy() {

  }

  @Override
  public E get(CharSequence key) {
    return null;
  }

  @Override
  public E get(CharSequence key, int start, int len) {
    return null;
  }

  @Override
  public E get(CharSequence key, StringTrieCursor<E> cursor,
    ReceivesValue<StringTrieCursor<E>> cursorPointer) {
    return null;
  }

  @Override
  public Iterable<E> findPrefixed(CharSequence key) {
    return null;
  }

  @Override
  public Iterable<E> findPrefixed(CharSequence key, int start, int len) {
    return null;
  }

  @Override
  public E put(CharSequence key, E vaue) {
    return null;
  }

  @Override
  public E put(CharSequence key, int start, int len, E value) {
    return null;
  }

  @Override
  public StringTrieCursor<E> put(CharSequence key, E vaue,
    StringTrieCursor<E> cursor) {
    return null;
  }

  @Override
  public StringTrieCursor<E> put(CharSequence key, int start, int len,
    E value, StringTrieCursor<E> cursor) {
    return null;
  }

  @Override
  public int size() {
    return 0;
  }

}
