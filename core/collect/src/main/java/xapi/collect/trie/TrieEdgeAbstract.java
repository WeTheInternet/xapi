package xapi.collect.trie;

import xapi.collect.api.StringTrie.StringTrieCursor;
import xapi.collect.api.StringTrie.StringTrieEdge;
import xapi.util.impl.Chars;

class TrieEdgeAbstract <E> implements StringTrieEdge<E> {

  private int depth;
  private Chars key;
  private E value;
  volatile StringTrieEdge<E> lesser;
  volatile StringTrieEdge<E> greater;

  public TrieEdgeAbstract(Chars key, int depth, E value) {
    this.key = key;
    this.depth = depth;
    this.value = value;
  }

  @Override
  public int depth() {
    return depth;
  }

  @Override
  public CharSequence key() {
    return key;
  }

  @Override
  public E value() {
    return value;
  }

  @Override
  public E addValue(CharSequence key, int keyFrom, E value) {
    return null;
  }

  @Override
  public StringTrieCursor<E> hasEdge(CharSequence key, int keyFrom) {
    return null;
  }

  @Override
  public StringTrieEdge<E> highest() {
    return greater;
  }

  @Override
  public StringTrieEdge<E> lowest() {
    return lesser;
  }

}