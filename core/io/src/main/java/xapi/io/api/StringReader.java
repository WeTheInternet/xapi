package xapi.io.api;

import xapi.collect.api.Fifo;
import xapi.collect.impl.SimpleFifo;
import xapi.log.X_Log;
import xapi.util.X_String;

/**
 * An implementation of {@link LineReader} designed to stream lines of text to one or more
 * delegate LineReaders.  It also stores all input so that a new LineReader can be added
 * at any time, and it will still receive all text that was streamed to this reader.
 * <p>
 * This is handy for such use cases as forwarding an external processes std in and std out,
 * where we might need to add a listener to the process after it has already started.
 * <p>
 * This class has also been enhanced with the {@link #waitToEnd()} method,
 * which will block until the line producer (such as a thread streaming input from an external process)
 * has signalled that the stream is finished by calling {@link #onEnd()}.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public class StringReader implements LineReader {
  private StringBuilder b;
  private final Fifo<LineReader> delegates = new SimpleFifo<LineReader>();
  boolean finished;

  @Override
  public void onStart() {
    b = new StringBuilder();
    if (delegates.isEmpty())return;
    for (LineReader delegate : delegates.forEach()) {
      delegate.onStart();
    }
  }
  @Override
  public void onLine(String line) {
    synchronized (this) {
      b.append(line);
      if (delegates.isEmpty())return;
      for (LineReader delegate : delegates.forEach()) {
        delegate.onLine(line);
      }
    }
  }

  @Override
  public final void onEnd() {
    X_Log.trace(getClass(),"ending", this);
    try {
      for (LineReader delegate : delegates.forEach()) {
        X_Log.debug(getClass(),"ending delegate", delegate.getClass(), delegate);
        delegate.onEnd();
      }
    } finally {
      finished = true;
      onEnd0();
    }
    synchronized (delegates) {
      delegates.notifyAll();
    }
  }

  protected void onEnd0() {
  }

  @Override
  public String toString() {
    return String.valueOf(b);
  }

  public synchronized void forwardTo(LineReader callback) {
    X_Log.debug(getClass(),getClass().getName(),"forwardingTo", callback.getClass().getName(),":", callback);
    if (b != null) {// not null only after we have started streaming
      callback.onStart();
      for (String line : X_String.splitNewLine(b.toString())) {
        callback.onLine(line);
      }
    }
    delegates.give(callback);
    if (finished) {
      callback.onEnd();
    }
  }

  public void waitToEnd() throws InterruptedException {
    if (finished) {
      return;
    }
    synchronized (delegates) {
      delegates.wait();
    }
  }

  public void waitToEnd(long timeout, int nanos) throws InterruptedException {
    if (finished) {
      return;
    }
    synchronized (delegates) {
      delegates.wait(timeout, nanos);
    }
  }

}
