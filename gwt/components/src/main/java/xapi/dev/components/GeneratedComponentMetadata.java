package xapi.dev.components;

import xapi.collect.X_Collect;
import xapi.collect.api.Fifo;
import xapi.fu.In1Out1;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 5/1/16.
 */
public class GeneratedComponentMetadata {

  protected final Fifo<In1Out1<String, String>> modifiers;
  private boolean allowedToFail;

  protected GeneratedComponentMetadata() {
    modifiers = newFifo();
    allowedToFail = Boolean.getBoolean("xapi.component.ignore.parse.failure");
  }

  protected Fifo<In1Out1<String, String>> newFifo() {
    return X_Collect.newFifo();
  }

  public boolean isAllowedToFail() {
    return allowedToFail;
  }
}
