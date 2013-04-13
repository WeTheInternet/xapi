package xapi.collect.trie;

import xapi.collect.api.StringTrie;
import xapi.collect.api.StringTrie.StringTrieCursor;
import xapi.collect.api.StringTrie.StringTrieEdge;
import xapi.util.impl.Chars;
import xapi.util.impl.Chars.SingleChar;

public class DeepTrieEdge <E> implements StringTrie.StringTrieEdge<E>{

  private static final SingleChar[] COMMON_CHARS = new SingleChar['_'];
  static {
    for (int i = '_';i-->0;)
      COMMON_CHARS[i] = new SingleChar((char)i);
  }

  protected class DeepTrieCursor implements StringTrie.StringTrieCursor<E> {

    private final SingleChar key;
    private final StringTrieEdge<E> edge;

    public DeepTrieCursor(char k, StringTrieEdge<E> edge) {
      if (k < COMMON_CHARS.length) {
        this.key = COMMON_CHARS[k];
      } else {
        this.key = new Chars.SingleChar(k);
      }
      this.edge = edge;
    }

    @Override
    public final CharSequence key() {
      return key;
    }

    @Override
    public final int consumed() {
      return 1;
    }

    @Override
    public final StringTrieEdge<E> edge() {
      return edge;
    }

  }

  //The depth, in characters, for this element
  private final int depth;
  //The sequence of characters leading up to this node
  private final CharSequence key;
  //The value, if any, stored at this node
  private volatile E value;

  //Remember our highest and lowest chars to make add-to-either-end efficient
  private volatile char lowest;
  private volatile char highest;
  //These are counts for the number of lower and higher elements
  //We use char because there cannot be more array indices than there are chars.
  private volatile char lowerUsed;
  private volatile char higherUsed;
//  // A cursor for speedy gets of the same value
//  // This is not volatile because thread caching here is good.
//  private int lastGet;
  //The array
  private StringTrieEdge<E>[] edges;

  public DeepTrieEdge(CharSequence key, int depth, E value) {
    this(key, depth, value, 9);
  }
  @SuppressWarnings("unchecked")
  public DeepTrieEdge(CharSequence key, int depth, E value, int initialSize) {
    this.depth = depth;
    this.key = key;
    this.value = value;
    this.edges = new StringTrieEdge[initialSize/2];
  }

  public synchronized DeepTrieEdge<E> addEdge(StringTrieEdge<E> edge) {
    final CharSequence key = edge.key();
    if (key.length() == 0) {
      //we're adding another deep edge
    } else {
      final char k = key.charAt(0);
      final int deltaLower = k - lowest;
      if (deltaLower < 0) {
        // we have a new lowest

        lowest = k;
      }
      final int deltaHigher = k - highest;
      if (deltaHigher > 0) {
        // we have a new highest.  First insert will go here.

        highest = k;
      }
      // no easy insert; perform binary search
      int pos = lowerUsed - 1;
      StringTrieEdge<E> biggestLow = edges[pos];
      int deltaBottom;
      searchLower:
      if (biggestLow == null) {
        deltaBottom = Integer.MIN_VALUE;
      } else {
        assert biggestLow.key().length() > 0;
        char test = biggestLow.key().charAt(0);
        deltaBottom = k - test;
        if (deltaBottom == 0) {// optimistic search
          addInto(biggestLow, edge);
          return this;
        }
        if (deltaBottom < 0) {
          // no need to consult highest.  search down from our "highest lowest"
          while (test > 0) {
            if (--pos < 0)
              break searchLower;
            biggestLow = edges[pos];
            if (biggestLow == null)
              break searchLower;
            test = biggestLow.key().charAt(0);
            deltaBottom = k - test;
            if (deltaBottom == 0) {
              addInto(biggestLow, edge);
              return this;
            }
            if (deltaBottom > 0) {
              // inserted node is higher than this lowest
              addAfterLower(biggestLow, edge, pos);
              return this;
            }
          }
        }
      }

      // Nothing found in lesser
      // Start searching the higher
      pos = edges.length - higherUsed;
      StringTrieEdge<E> lowestHigh = edges[pos];
      int deltaTop;
      searchHigher:
      if (lowestHigh != null) {
        assert lowestHigh.key().length() > 0;
        char test = lowestHigh.key().charAt(0);
        deltaTop = k - test;
        if (deltaTop== 0) {// optimistic search
          addInto(lowestHigh, edge);
          return this;
        }
        if (deltaTop > 0) {
          // we are greater than the lowest high value, so we must search
          while (test > 0) {
            if (++pos >= edges.length)
              break searchHigher;
            lowestHigh = edges[pos];
            if (lowestHigh == null)
              break searchHigher;
            test = lowestHigh.key().charAt(0);
            deltaTop = k - test;
            if (deltaTop== 0) {
              addInto(lowestHigh, edge);
              return this;
            }
            if (deltaTop < 0) {
              // inserted node is less than this highest
              addBeforeHigher(lowestHigh, edge, pos);
              return this;
            }
          }
          // Inserted edge is greater
        }
      }
      // Neither top nor bottom encloses this char,
      // And it's not a max or min, thus, it must be between higher and lower.
      // We choose to add it to the side that has the fewest elements
      // TODO finish this
    }
    return this;
  }

  private void addBeforeHigher(StringTrieEdge<E> biggestLow, StringTrieEdge<E> edge, int pos) {

  }
  private void addAfterLower(StringTrieEdge<E> biggestLow, StringTrieEdge<E> edge, int pos) {

  }
  private void addInto(StringTrieEdge<E> into, StringTrieEdge<E> edge) {

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
    if (key.length() == 0 || keyFrom == key.length() - 1) {
      E ret = this.value;
      this.value = value;
      return ret;
    }
    StringTrieCursor<E> cursor = hasEdge(key, keyFrom);
    if (cursor == null) {

    } else {
      StringTrieEdge<E> into = cursor.edge();
      return into.addValue(key, keyFrom+1, value);
    }

    return null;
  }
  @Override
  public StringTrieCursor<E> hasEdge(CharSequence key, int keyFrom) {
    return null;
  }
  @Override
  public StringTrieEdge<E> highest() {
    return edges[edges.length-1];
  }
  @Override
  public StringTrieEdge<E> lowest() {
    return edges[0];
  }

}
