package xapi.util.tools;

import xapi.fu.Rethrowable;
import xapi.util.X_Debug;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/9/16.
 */
public interface DebugRethrowable extends Rethrowable {

  @Override
  default RuntimeException rethrow(Throwable e) {
    X_Debug.debug(e);
    return Rethrowable.super.rethrow(e);
  }
}
