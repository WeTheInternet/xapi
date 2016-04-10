package xapi.fu;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 07/11/15.
 */
public interface Rethrowable {

  default RuntimeException rethrow(Throwable e) {
    if (e instanceof RuntimeException) {
      throw (RuntimeException)e;
    }
    if (e instanceof Error) {
      throw (Error)e;
    }
    throw newRuntimeException(e);
  }

  default RuntimeException newRuntimeException(Throwable e) {
    return new RuntimeException(e);
  }

}
