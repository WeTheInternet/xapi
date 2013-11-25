package xapi.io.api;

import xapi.collect.api.Fifo;
import xapi.collect.impl.SimpleFifo;
import xapi.log.X_Log;
import xapi.util.X_String;

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
      if (b.length() > 0) {
        b.append('\n');
      }
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
    if (b != null) {
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