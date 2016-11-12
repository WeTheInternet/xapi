package xapi.fu;

import xapi.fu.Log.LogLevel;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 07/11/15.
 */
public interface Rethrowable {

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
  default RuntimeException rethrow(Throwable e) {
    if (this instanceof Debuggable) {
      ((Debuggable)this).viewException(this, e);
    }
    if (e instanceof RuntimeException) {
      throw (RuntimeException)e;
    }
    if (e instanceof Error) {
      throw (Error)e;
    }
//    if (e instanceof InvocationTargetException && e.getCause() != null) {
//      e = e.getCause();
//    }
//    if (e instanceof ExecutionException && e.getCause() != null) {
//      e = e.getCause();
//    }
    if (e instanceof InterruptedException) {
      Thread.currentThread().interrupt();
    }
    throw newRuntimeException(e);
  }

  default RuntimeException newRuntimeException(Throwable e) {
    return new RuntimeException(e);
  }

}
