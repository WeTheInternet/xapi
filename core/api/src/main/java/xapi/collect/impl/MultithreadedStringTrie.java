package xapi.collect.impl;


import java.io.Serializable;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;

import static xapi.collect.api.CharPool.EMPTY_STRING;

import xapi.collect.api.CharPool;
import xapi.util.impl.Chars;

/**
 * NSFW
 * This somewhat monsterous Trie implementation is a work in progress.
 * <p>
 * It exists in a cleaner form in the labs module, but it was good enough
 * to act as a high-performance solution in multi-threaded environments.
 * <p>
 * It is really only used for our classpath scanner,
 * which can beat java.util concurrent maps in heavily threaded enviros
 * <p>
 * Trie walking locks safely, but with minimal hold times, so contention
 * during concurrent operations will only occur if the keys overlap.
 * Once one thread is ahead of another, it will stay ahead of others.
 * <p>
 * This class should NOT be used anywhere that doesn't explicitly need Trie
 * behavior in multi-threaded enviros.  It's not optimized for lookup time yet.
 * <p>
 * Application-wide, java-package based keys are good candidates for StringTrie.
 * <p>
 * Production Trie versions will be suitable for tasks like type-ahead,
 * with O(log(m)) lookup, where m = string length instead of n = collection size.
 * <p>
 * The in-progress upgrade to this class will include array-based deep node
 * support, so nodes with more than two branches can do faster lookup.
 * <p>
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 * @param <E>
 */
public class MultithreadedStringTrie<E> {

  private static final char[] emptyString = new char[0];

  /**
   * Our Edge class is one node in the Trie graph. It is mutable so we can keep
   * our memory impact light, and volatile so we can stay threadsafe. All of
   * this is at relatively no extra cost to processing time, except for
   * synchronization time on acquiring downward locks during puts (and a little
   * extra processing time for deletes to acquire locks as well). Gwt doesn't
   * pay for synchronization, so it performs optimally in js. Note that it does
   * not hold a parent lock while taking a child lock, so multiple threads can
   * still quickly transverse any potential hotspots, where there is alot of
   * prefix-overlap in strings, such as java packages: com.foo.client.Something
   * com.foo.client.SomethingElse com.foo.server.Something
   * com.bar.client.Something com.bar.server.Something ...etc. The fragment com.
   * would be locked and released before acquiring foo. | bar. This prevents
   * concurrent modifications in different areas of the trie to avoid blocking
   * each other.
   *
   * @author "James X. Nelson (james@wetheinter.net)"
   */
  public class Edge implements Serializable {
    private static final long serialVersionUID = 5885970862972987462L;

    protected E value;
    protected volatile Edge greater;
    protected volatile Edge lesser;
    // use char[] instead of string for optimized .toString() on keys.
    // we have to build keysets if requested,
    // and the only way to avoid using buffers or power-of-two guessing,
    // is to iteratively assemble exact length char[]s when assembling keys.
    // this allows parent char[]s to be built once per all their children.
    protected volatile char[] key;

    protected Edge() {
      this(emptyString, 0, 0);
    }

    public Edge(final char[] key, final int index, final int end) {
      if (index == 0 && end == key.length) {
        this.key = key;
        assert key == emptyString || end > 0;
      } else {
        this.key = new char[end - index];
        assert this.key.length > 0;
        System.arraycopy(key, index, this.key, 0, this.key.length);
      }
    }

    protected void setValue(E value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return new String(key);
    }
  }

  class Stack {
    Stack next;
    Edge value;

    public Stack(Stack stack, Edge value) {
      this.next = stack;
      this.value = value;
    }
  }

  protected class Itr implements Iterator<E> {

    Edge next;
    Stack stack = new Stack(null, null);

    public Itr(String prefix) {
      int pos = 0;
      next = root;
      for (int m = prefix.length(); pos < m;) {
        char[] key;
        char k = prefix.charAt(pos);

        if (next.lesser != null) {
          key = next.lesser.key;
          if (k == key[0]) {
            int match = 0;
            while (++match < key.length) {
              if (key[pos + match] != key[match]) {
                if (next.greater != null) {
                  stack = new Stack(stack, next.greater);
                }
                next = next.lesser;
                return;
              }
            }
            pos += match;
            next = next.lesser;
            continue;
          }
        }
        if (next.greater != null) {
          key = next.greater.key;
          if (key.length == 0) {
            next = next.greater;
            continue;
          } else {
            if (k == key[0]) {
              int match = 0;
              while (++match < key.length) {
                if (key[pos + match] != key[match]) {
                  next = next.greater;
                  return;
                }
              }
              pos += match;
              next = next.greater;
              continue;
            }
          }
        }
        next = null;
        return;
      }
    }
    E value;
    @Override
    public boolean hasNext() {
      if (next == null) return false;
      while (stack != null) {
        if (next.value != null) {
          value = next.value;
          if (next.lesser == null) {
            if (next.greater == null) {
              next = stack.value;
              stack = stack.next;
            } else {
              next = next.greater;
            }
          } else if (next.greater == null) {
            next = stack.value;
            stack = stack.next;
          } else {
            next = next.greater;
          }
          return true;
        }
        if (next.lesser == null) {
          assert next.greater == null;
          next = stack.value;
          stack = stack.next;
        } else {
          if (next.greater != null)
            stack = new Stack(stack, next.greater);
          next = next.lesser;
        }
      }
      return false;
    }

    @Override
    public E next() {
      return value;
    }

    @Override
    public void remove() {

    }

  }

  // let subclasses do stuff. At least it's final...
  protected final Edge root = newEdge();

  public void put(final char[] key, final int start, final int end, final E value) {
    if (key == null || key.length == 0) {
      root.setValue(value);
    } else {
      if (start < 0 || end > key.length) throw new ArrayIndexOutOfBoundsException();
      doPut(root, key, start, end, value);
    }
  }

  public void put(final String key, final E value) {
    if (key == null || "".equals(key))
      root.setValue(value);
    else
      doPut(root, key.toCharArray(), 0, key.length(), value);
  }

  protected void doPut(final Edge into, final char[] key, final int index, final int end, final E value) {
    assert index < end;
    // To stay threadsafe, we synchronize on Edges when we modify them.
    // To stay fast, we don't recurse until we are out of the synchro block.

    // We optimize for our worst-case scenario off the hop;
    // which is a deep node transversal (when one node points to many).
    final Edge nextInto;
    int nextIndex;

    final char k = key[index];
    // handle peeking into deeper nodes that will result in recursion.
    final Edge greater = into.greater;
    if (greater != null) {
      assert into.lesser != null;
      final char[] greaterKey = greater.key;
      // deep nodes are stored in greater slot
      if (greaterKey.length == 0) {// this is a deep node!
        // bounds check on its lesser
        if (k - greater.lesser.key[0] >= 0) {
          // if inserted key is not less than the lesser of the deep node,
          // then recurse into the greater, without locking.
          doPut(greater, key, index, end, value);
          return;
        }

        // we are in a deep node, and are less than the greater.
        // check if we need to insert a new deep node.
        synchronized (into) {
          if (greater == into.greater) {
            // The only comod we need to worry about here is the greater node;
            // if the lesser is changed while we were waiting, we're still okay.
            final Edge lesser = into.lesser;
            final int delta = k - lesser.key[0];
            if (delta != 0) {
              final Edge newParent = newEdge();
              newParent.greater = into.greater;

              final Edge newNode = newEdge(key, index, end, value);

              if (delta > 0) {
                // new node is greater than current lesser; replace into.greater
                newParent.lesser = newNode;
              } else {
                // new node is less than our lesser; take lesser spot
                // and make the old lesser a new deep node
                newParent.lesser = lesser;
                into.lesser = newNode;
              }
              into.greater = newParent;
              return;// done!
            }
            // we start with the same char as into.lesser;
            // find out how far we match, and possibly recurse.
            if (insertLesser(into, key, index, end, value)) return;
            // if we didn't return, we must recurse into this lesser
            nextInto = into.lesser;
            nextIndex = index + lesser.key.length;
          } else {
            // the trie was modified while we were waiting,
            // recurse, as we need to run the deep checks again.
            nextInto = into;
            nextIndex = index;
          }
        }// end synchro
         // if we didn't return, we need to recurse.
        if (nextIndex == end) {
          nextInto.setValue(value);
        } else {
          doPut(nextInto, key, nextIndex, end, value);
        }
        return;
      }// end deep node
    }// end into.greater != null

    // because we are only locking on the parent node,
    // but potentially modifying the structure of child nodes,
    // and we don't want to invite deadlock, we only ever iterate downward;
    // we acquire the locks on children before modifying them
    // or accessing the lesser / greater nodes during a put.
    synchro: synchronized (into) {
      // into.lesser will only ever be null on the very first put.
      if (into.lesser == null) {
        assert into.greater == null;
        // both null, just take lesser and exit
        into.lesser = newEdge(key, index, end, value);
        return;
      }
      // start our compare on lesser...
      final char[] lesserKey = into.lesser.key;
      final int deltaLesser = k - lesserKey[0];
      if (deltaLesser == 0) {
        // we match the first char of the lesser.
        if (insertLesser(into, key, index, end, value)) return;
        // if we didn't return, we must recurse
        nextInto = into.lesser;
        nextIndex = index + lesserKey.length;
        break synchro; // never recurse inside a synchro
      }
      // if we are less than the lesser, we need to usurp its position
      if (into.greater == null) {
        // with no greater node, our job is easy. Just fill this node up.
        final Edge newNode = newEdge(key, index, end, value);
        if (deltaLesser < 0) {
          into.greater = into.lesser;
          into.lesser = newNode;
        } else {
          into.greater = newNode;
        }
        return;
      }

      // we have to check the greater,
      // which may have changed since we last deep-checked it...
      final char[] greaterKey = into.greater.key;
      if (greaterKey.length == 0) {
        // the greater is now deep and it wasn't before.
        // recurse back into the same node; we can't get back here once deep
        nextInto = into;
        nextIndex = index;
        break synchro;
      }

      if (deltaLesser < 0) {
        // A greater exists, but we still need to usurp lesser
        final Edge newParent = newEdge();
        final Edge newNode = newEdge(key, index, end, value);
        newParent.lesser = into.lesser;
        newParent.greater = into.greater;
        into.greater = newParent;
        into.lesser = newNode;
        return;
      }

      // The only thing left to do is run a compare on greater
      final int deltaGreater = k - greaterKey[0];
      if (deltaGreater == 0) {
        // we must insert into the greater, or else recurse
        if (insertGreater(into, key, index, end, value)) return;
        nextInto = into.greater;
        nextIndex = index + into.greater.key.length;
        break synchro;
      }
      // we don't start with greater or lesser, and must create a deep node
      final Edge newParent = newEdge();
      final Edge newNode = newEdge(key, index, end, value);
      if (deltaGreater > 0) {
        // new node is the greatest
        newParent.greater = newNode;
        newParent.lesser = into.greater;
      } else {
        newParent.greater = into.greater;
        newParent.lesser = newNode;
      }
      into.greater = newParent;
      return;
    }// end synchro. If we haven't returned, we need to recurse.
    if (nextIndex == end) {
      nextInto.setValue(value);
    } else {
      doPut(nextInto, key, nextIndex, end, value);
    }
  }

  protected Edge newEdge() {
    return new Edge();
  }

  protected Edge newEdge(char[] key, int index, int end, E value) {
    Edge e = new Edge(key, index, end);
    e.setValue(value);
    return e;
  }

  private boolean insertLesser(Edge into, char[] key, int index, int end, E value) {
    int matchesTo = 1;// only called when we've already matched the first char
    final int keyLen = end - index;
    final char[] lesserKey = into.lesser.key;
    for (; matchesTo < keyLen; matchesTo++) {
      if (matchesTo == lesserKey.length) {
        return false;
      }
      final int delta = key[index + matchesTo] - lesserKey[matchesTo];
      if (delta < 0) {
        // new node is less than lesser
        into.lesser = newEdgeLesser(into.lesser, keyLen, lesserKey, matchesTo, key, index, end, value);
        return true;
      }
      if (delta > 0) {
        // new node is greater than lesser
        into.lesser = newEdgeGreater(into.lesser, keyLen, lesserKey, matchesTo, key, index, end, value);
        return true;
      }
    }
    if (matchesTo == lesserKey.length) {
      return false;
    }
    // If we haven't returned, than the existing key is longer than the one
    // we are inserting. Thus, we must slip the new node behind the old one.
    final Edge newNode = newEdge(key, index, end, value);
    final char[] newLesser = new char[lesserKey.length - keyLen];
    System.arraycopy(lesserKey, keyLen, newLesser, 0, newLesser.length);
    newNode.lesser = into.lesser;
    into.lesser = newNode;
    newNode.lesser.key = newLesser;
    return true;
  }

  private boolean insertGreater(Edge into, char[] key, int index, int end, E value) {
    int matchesTo = 1;// only called when we've already matched the first char
    final int keyLen = end - index;
    final char[] greaterKey = into.greater.key;
    for (; matchesTo < keyLen; matchesTo++) {
      if (matchesTo == greaterKey.length) {
        return false;
      }
      final int delta = key[index + matchesTo] - greaterKey[matchesTo];
      if (delta < 0) {
        // new node is less than greater
        into.greater = newEdgeLesser(into.greater, keyLen, greaterKey, matchesTo, key, index, end, value);
        return true;
      }
      if (delta > 0) {
        // new node is greater than lesser
        into.greater = newEdgeGreater(into.greater, keyLen, greaterKey, matchesTo, key, index, end, value);
        return true;
      }
    }
    if (matchesTo == greaterKey.length) {
      return false;
    }
    // If we haven't returned, than the existing key is longer than the one
    // we are inserting. Thus, we must slip the new node behind the old one.
    final Edge newNode = newEdge(key, index, end, value);
    final char[] newGreater = new char[greaterKey.length - keyLen];
    System.arraycopy(greaterKey, keyLen, newGreater, 0, newGreater.length);
    newNode.greater = into.greater;
    into.greater = newNode;
    newNode.greater.key = newGreater;
    return true;
  }

  protected Edge newEdgeLesser(Edge previous, int keyMax, char[] existing, int matchesTo, char[] key,
    int keyIndex, int keyEnd, E value) {
    // found our break point
    final char[] newRootKey = new char[matchesTo];
    final char[] newExistingKey = new char[existing.length - newRootKey.length];
    final char[] newInsertedKey = new char[keyMax - newRootKey.length];

    // copy the common root into our new parent edge
    System.arraycopy(existing, 0, newRootKey, 0, newRootKey.length);
    final Edge newRoot = newEdge(newRootKey, 0, newRootKey.length, null);

    // trim the existing key to it's unique suffix value
    System.arraycopy(existing, newRootKey.length, newExistingKey, 0, newExistingKey.length);
    previous.key = newExistingKey;

    // create a new node for our value
    System.arraycopy(key, keyIndex + newRootKey.length, newInsertedKey, 0, newInsertedKey.length);
    final Edge newEdge = newEdge(newInsertedKey, 0, newInsertedKey.length, value);

    assert newRoot.key.length > 0;
    assert previous.key.length > 0;
    assert newEdge.key.length > 0;

    newRoot.lesser = newEdge;
    newRoot.greater = previous;
    assert newEdge.toString().compareTo(previous.toString()) < 0 : "Invalid greaterthan: " + newEdge +
      " is not < " + previous;
    return newRoot;
  }

  protected Edge newEdgeGreater(Edge previous, int keyMax, char[] existing, int matchesTo, char[] key,
    int keyIndex, int keyEnd, E value) {
    // found our break point
    final char[] newRootKey = new char[matchesTo];
    final char[] newExistingKey = new char[existing.length - newRootKey.length];
    final char[] newInsertedKey = new char[keyMax - newRootKey.length];

    // copy the common root into our new parent edge
    System.arraycopy(existing, 0, newRootKey, 0, newRootKey.length);
    final Edge newRoot = newEdge(newRootKey, 0, newRootKey.length, null);

    // trim the existing key to it's unique suffix value
    System.arraycopy(existing, newRootKey.length, newExistingKey, 0, newExistingKey.length);
    previous.key = newExistingKey;

    // create a new node for our value
    System.arraycopy(key, keyIndex + newRootKey.length, newInsertedKey, 0, newInsertedKey.length);
    final Edge newEdge = newEdge(newInsertedKey, 0, newInsertedKey.length, value);

    assert newRoot.key.length > 0;
    assert previous.key.length > 0;
    assert newEdge.key.length > 0;

    newRoot.lesser = previous;
    newRoot.greater = newEdge;
    assert newEdge.toString().compareTo(previous.toString()) > 0;

    return newRoot;
  }

  /**
   * @param into - The edge to lock
   * @param ownsParent - Whether we already own an explicit lock on the parent.
   * @return - Any object you want; null will do fine. This method is provided
   * as a stub for more sophisticated, concurrent subclasses which may want to
   * employ locking mechanisms (or event dispatch). You may call
   * {@link Object#wait(long, int)}; as you already own the lock. long param is
   * millis, should be zero. int param is nanos, keep it in the hundreds. DON'T
   * DO ANYTHING WHICH COULD BLOCK FOR A LONG TIME. Acquire locks tentatively,
   * either with {@link Lock#tryLock()} for failfast, or
   * {@link Lock#tryLock(long, java.util.concurrent.TimeUnit)}. Wait times, if
   * any, should be on a nano scale; If ownsParent is false, you should be
   * running in unsynchronized code. The only use for synchronous method blocks
   * in this case is to acquire a {@link Lock}. If ownsParent is true, you are
   * safe from intrusion from above (nobody will be able to modify your parent),
   * but you still have to contend
   */
  protected void lock(Edge into, boolean ownsParent) {
  }

  /**
   * @param into - The edge to lock
   * @param ownsParent - If true, you are already synchronized on into.
   * @param cursor - Whatever object you returned when you locked. This method
   * is a stub for more sophisticated subclasses of StringTrie, which may need
   * to perform proper concurrent locking, or event dispatch. It is called in
   * the finally block of whatever code ran
   * {@link MultithreadedStringTrie#lock(Edge, boolean)}. If you use Edge
   * into.wait(0, nanos) in lock(), now would be a great time to call into
   * into.notify() :)
   */
  protected void unlock(Edge into, boolean ownsParent) {
  }

  @Override
  public String toString() {
    final StringBuilder b = new StringBuilder();
    b.append("StringTrie[\n");
    if (root.value != null) {
      b.append("\"\" : " + root.value + "\n");
    }
    if (root.greater != null) {
      visit(root.greater, 1, new char[0], b);
    }
    if (root.lesser != null) {
      visit(root.lesser, 1, new char[0], b);
    }
    b.append("]");
    return b.toString();
  }

  private void visit(Edge root, int depth, char[] key, StringBuilder b) {
    boolean anyKey = key.length > 0;
    if (root.key.length > 0) {
      for (int i = 0; i < depth; i++) {
        b.append(' ');
      }
      b.append(root.key);
      b.append("\t\t");
      if (root.value == null) {
        b.append("[branch]");
      } else {
        b.append(root.value);
      }
      b.append('\n');
    }
    if (root.lesser != null) {
      // visit lesser edge
      char[] childKey = root.lesser.key;
      if (anyKey) {
        // assert childKey.length > 0 : b;
        char[] nextKey = new char[key.length + childKey.length];
        System.arraycopy(key, 0, nextKey, 0, key.length);
        System.arraycopy(childKey, 0, nextKey, key.length, childKey.length);
        childKey = nextKey;
        nextKey = null;
      }
      visit(root.lesser, depth + (anyKey ? 1 : 0), childKey, b);
      childKey = null;
    }
    if (root.greater != null) {
      // visit greater edge
      char[] childKey = root.greater.key;
      if (anyKey) {
        char[] nextKey = new char[key.length + childKey.length];
        System.arraycopy(key, 0, nextKey, 0, key.length);
        System.arraycopy(childKey, 0, nextKey, key.length, childKey.length);
        childKey = nextKey;
        nextKey = null;
      }
      boolean addSpace = anyKey && root.greater.key.length > 0;
      visit(root.greater, depth + (addSpace ? 1 : 0), childKey, b);
      childKey = null;
    }
  }

  public E get(String key) {
    if (key == null) return get(EMPTY_STRING);
    return get(new Chars(key.toCharArray()), 0, key.length());
  }

  public E get(char[] key) {
    if (key == null) key = EMPTY_STRING;
    return get(new Chars(key), 0, key.length);
  }

  public E get(char[] key, int pos, int end) {
    if (key == null) key = EMPTY_STRING;
    return get(new Chars(key, pos, end), pos, end);
  }

  public E get(final Chars keys, int pos, final int end) {
    Edge e = root;
    while (e != null) {
      // our test for success is always when we make it through a for loop
      // which matches our key, and when the next search position = key length.
      // if there was a value at this key, we would have returned it.
      if (pos == end) return returnValue(e, keys, pos, end);

      if (e.lesser != null) {
        final char[] lesser = e.lesser.key;
        testlesser: {
          for (int i = 0; i < lesser.length; i++) {
            if (end <= pos + i) return onEmpty(e, keys, pos, end);
            final int delta = keys.charAt(pos + i) - lesser[i];
            if (delta < 0) {
              // if a lesser is greater than us, there's nothing to return
              return onEmpty(e, keys, pos, end);
            }
            if (delta > 0) {
              break testlesser;
            }
          }// end for
           // if we didn't break, we equal the lesser. Descend into it.
          e = e.lesser;
          pos += lesser.length;
          continue;
        }// end test lesser
         // requested key is greater than lesser key. Carry on.
      }// end lesser

      if (e.greater == null) return onEmpty(e, keys, pos, end);
      final char[] greater = e.greater.key;
      if (greater.length == 0) {
        // deep node, just continue search
        e = e.greater;
        continue;
      }
      final int len = greater.length;
      if (len + pos > end) return onEmpty(e, keys, pos, end);
      for (int i = 0; i < len; i++) {
        if (keys.charAt(pos + i) != greater[i]) return onEmpty(e, keys, pos, end);
      }
      pos += len;
      // still haven't returned, so we match this greater
      e = e.greater;
    }
    return onEmpty(e, keys, pos, end);
  }

  public Iterable<E> findPrefixed(String prefix) {
    return new IteratorWrapper<E>(new Itr(prefix));
  }

  public void compress(CharPool charPoolTrie) {

  }

  protected E returnValue(Edge e, Chars keys, int pos, int end) {
    return e.value;
  }

  protected E onEmpty(Edge e, Chars keys, int pos, int end) {
    return null;
  }

}
