/**
 *
 */
package xapi.util.impl;

import static xapi.util.X_Debug.rethrow;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public interface RunUnsafe {

  void doRun() throws Throwable;

  default Runnable asRunnable() {
    return () -> {
      try {
        doRun();
      } catch(final Throwable e) {
        throw rethrow(e);
      }
    };
  }
}
