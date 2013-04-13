package xapi.io.api;

import xapi.collect.api.Fifo;
import xapi.collect.impl.SimpleFifo;
import xapi.util.X_String;

public class StringReader implements LineReader {
  private StringBuilder b;
  private Fifo<LineReader> delegates = new SimpleFifo<LineReader>();

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
      b.append('\n');
      if (delegates.isEmpty())return;
      for (LineReader delegate : delegates.forEach()) {
        delegate.onLine(line);
      }
    }
  }
  @Override
  public void onEnd() {
    b = null;
    if (delegates.isEmpty())return;
    for (LineReader delegate : delegates.forEach()) {
      delegate.onEnd();
    }
  }
  @Override
  public String toString() {
    return String.valueOf(b);
  }

  public synchronized void forwardTo(LineReader callback) {
    if (b == null) {
      delegates.give(callback);
    } else {
      callback.onStart();
      for (String line : X_String.splitNewLine(b.toString())) {
        callback.onLine(line);
      }
      delegates.give(callback);
    }
  }
}