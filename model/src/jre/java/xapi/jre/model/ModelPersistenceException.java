package xapi.jre.model;

import java.io.IOException;
import java.nio.file.Path;

/** A file replacement was attempted; the new value may be visible but durability is unconfirmed. */
public final class ModelPersistenceException extends IOException {

  public ModelPersistenceException(Path target, IOException cause) {
    super("Model replacement outcome or durability is uncertain: " + target, cause);
  }

  public boolean isCommitUncertain() {
    return true;
  }
}
