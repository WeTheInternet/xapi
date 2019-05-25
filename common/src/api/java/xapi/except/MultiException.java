package xapi.except;

import xapi.fu.itr.Chain;
import xapi.fu.itr.ChainBuilder;
import xapi.fu.itr.MappedIterable;

public class MultiException extends RuntimeException{

  private static final long serialVersionUID = -7586290946685197307L;
  private ChainBuilder<Throwable> throwables = Chain.startChain();

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
    throwables.add(exception);
  }

  public MappedIterable<Throwable> getThrowables() {
    return throwables;
  }

    @Override
    public void printStackTrace() {
        super.printStackTrace();
        throwables.forAll(Throwable::printStackTrace);
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
