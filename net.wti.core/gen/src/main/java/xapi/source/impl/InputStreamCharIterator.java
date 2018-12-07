package xapi.source.impl;

import xapi.fu.Rethrowable;
import xapi.fu.in.ReadAllInputStream;
import xapi.source.api.CharIterator;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.concurrent.locks.LockSupport;

public class InputStreamCharIterator implements CharIterator, Rethrowable {

  private InputStream in;
  private int length;
  private volatile Character next;

  public InputStreamCharIterator(final InputStream in) {
    this(in, Integer.MAX_VALUE);
  }

  public InputStreamCharIterator(final InputStream in, int limit) {
    if (limit == -1) {
      // If you don't know the input, we have to read the whole stream.
      final ReadAllInputStream all = ReadAllInputStream.read(in);
      this.in = all;
      this.length = all.available();
    } else {
      this.in = in;
      length = limit;
    }
  }

  public InputStreamCharIterator setLimit(int limit) {
    this.length = limit;
    return this;
  }

  @Override
  public char next() {
    if (next == null) {
      peek();
    }
    char n = next;
    next = null;
    return n;
  }

  @Override
  public char peek() {
    if (next == null) {
      try {
        int i = in.read();
        if (i == -1) {
          throw new IndexOutOfBoundsException("Requested more chars than were available");
        }
        length--;
        next = maybeReadMore(i, in);
        if (next != i) {
          length--;
        }
        if (length < 0) {
          throw new IndexOutOfBoundsException("Requested more chars than were available");
        }
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
    return next;
  }

  /**
   * For subclassers who want to handle more than ascii,
   * this is where you can detect a high surrogate value;
   * you may read more items from the input stream to construct a valid char
   */
  protected char maybeReadMore(int i, InputStream in) {
    // ascii...
    return (char)i;
  }

  public String peekString() {
    return Character.toString(peek());
  }

  @Override
  public boolean hasNext() {
    try {
      if (next == null) {
        int n = in.read();
        if (n == -1) {
          return false;
        }
        next = maybeReadMore(n, in);
      }
      return next != null;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public String toString() {
    final ReadAllInputStream all = ReadAllInputStream.read(in);
    in = all;
    return all.available() == 0 ? "" : new String(all.all());
  }

  @Override
  public CharSequence consume(int size) {
    StringBuilder b = new StringBuilder();
    while(size-->0) {
      b.append(next());
    }
    return b;
  }

  @Override
  public CharSequence consumeAll() {
    StringBuilder b = new StringBuilder();
    while(hasNext()) {
      b.append(next());
    }
    return b;
  }
}
