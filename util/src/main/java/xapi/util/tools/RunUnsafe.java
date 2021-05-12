/**
 *
 */
package xapi.util.tools;

import static xapi.debug.X_Debug.rethrow;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public abstract class RunUnsafe {

  protected abstract void doRun() throws Throwable;

  public Runnable asRunnable() {
    return new Runnable() {
      @Override
      public void run() {
        try {
          doRun();
        } catch(final Throwable e) {
          throw rethrow(e);
        }
      }
    };
  }
}
