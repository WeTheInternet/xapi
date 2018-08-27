package xapi.fu;

import xapi.fu.Log.LogLevel;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.ExecutionException;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 07/11/15.
 */
public interface Rethrowable {

  Rethrowable DEFAULT_RETHROW = new Rethrowable() {};

  /**
   * This method exists for cases when you want to override invocations to rethrow;
   * it allows you to send an arbitrary object along when rethrowing, so you can inspect that object
   * to decide if you actually want to throw or not.
   *
   * Note that you must still return a RuntimeException, as code that must break on an error
   * will manually call `throw rethrow(...)`.
   *
   * @param context - An arbitrary object for you to send when rethrowing.
   * @param e - The exception you want to rethrow.
   * @return - A runtime exception, to allow for `throw rethrow(e)` to break control flow.
   *
   * Note that, by default, no exception is ever returned; we just rethrow the root cause of the exception.
   */
  default RuntimeException rethrowWithContext(Object context, Throwable e) {
    if (this instanceof Log) {
      ((Log)this).log(getClass(), LogLevel.DEBUG, "Rethrowing with context", context, e);
    }
    return rethrow(e);
  }
  default <T> T sneakyThrow(Throwable e) {
    throw rethrow(e);
  }
  default RuntimeException rethrowCause(Throwable e) {
    return rethrow(e.getCause() == null ? e : e.getCause());
  }
  default RuntimeException rethrow(Throwable e) {
    if (this instanceof Debuggable) {
      ((Debuggable)this).viewException(this, e);
    }
    if (e instanceof RuntimeException) {
      throw X_Fu.rethrow(e);
    }
    if (e instanceof Error) {
      throw X_Fu.rethrow(e);
    }
    boolean hasCause = e.getCause() != null;
    if (hasCause) {

      if (e instanceof InvocationTargetException) {
        // TODO: addSuppressed? Pretty sure that causes an infinite loop of death when printing out...
        e = e.getCause();
      }
      if (e instanceof UndeclaredThrowableException) {
        e = e.getCause();
      }
      if (e instanceof ExecutionException) {
        e = e.getCause();
      }
    }
    if (e instanceof InterruptedException) {
      Thread.currentThread().interrupt();
    }
    final RuntimeException override = newRuntimeException(e);
    if (override.getClass() != RuntimeException.class || override.getMessage() != null) {
      // Only wrap in a new runtime exception if newRuntimeException was meaningfully overloaded
      throw override;
    }
    throw X_Fu.rethrow(e);
  }

  default RuntimeException newRuntimeException(Throwable e) {
    return new RuntimeException(e);
  }

    static <I1, I2, T> T throwNow(In2Out1<I1, I2, Throwable> exception, I1 arg1, I2 arg2) {
      return throwNow(exception.supply1(arg1).supply(arg2));
    }

    static <I1, T> T throwNow(In1Out1<I1, Throwable> exception, I1 arg) {
      return throwNow(exception.supply(arg));
    }
    static <T> T throwNow(Out1<Throwable> exception) {
      throw new Rethrowable(){}
        .rethrow(exception.out1());
    }

  static Rethrowable firstRethrowable(Object first) {
    if (first instanceof Rethrowable) {
      return (Rethrowable) first;
    }
    return DEFAULT_RETHROW;
  }
  static Rethrowable firstRethrowable(Object first, Object ... rest) {
    final Rethrowable winner = firstRethrowable(first);
    if (winner != DEFAULT_RETHROW) {
      return winner;
    }
    for (Object o : rest) {
      if (o instanceof Rethrowable) {
        return (Rethrowable) o;
      }
    }
    return DEFAULT_RETHROW;
  }
}
