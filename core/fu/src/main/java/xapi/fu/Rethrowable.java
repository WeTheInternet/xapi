package xapi.fu;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 07/11/15.
 */
public interface Rethrowable {

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
    if (e instanceof InterruptedException) {
      Thread.currentThread().interrupt();
    }
    throw newRuntimeException(e);
  }

  default RuntimeException newRuntimeException(Throwable e) {
    return new RuntimeException(e);
  }

}
