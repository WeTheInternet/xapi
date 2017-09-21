package xapi.except;

import xapi.collect.api.Fifo;
import xapi.collect.impl.SimpleFifo;

public class MultiException extends RuntimeException{

  private static final long serialVersionUID = -7586290946685197307L;
  private Fifo<Throwable> throwables = new SimpleFifo<Throwable>();

  public MultiException() {
  }

  public MultiException(String message) {
    super(message);
  }
  public MultiException(String message, Throwable ... exceptions) {
    super(message);
    for (Throwable exception : exceptions) {
      addThrowable(exception);
    }
  }

  public void addThrowable(Throwable exception) {
    throwables.give(exception);
  }

  public Iterable<Throwable> getThrowables() {
    return throwables.forEach();
  }

  @Override
  public String toString() {
    return super.toString() + "\n" +throwables.join("\n\n");
  }

  public static Throwable mergedThrowable(String msg, Throwable current, Throwable previous) {
      if (current == previous) {
        return current;
      }
      if (previous instanceof MultiException) {
        ((MultiException)previous).addThrowable(current);
        return previous;
      }
      if (current instanceof MultiException) {
        ((MultiException)current).addThrowable(previous);
        return current;
      }
      MultiException e = new MultiException(msg);
      e.addThrowable(previous);
      e.addThrowable(current);
      return e;
  }
}
