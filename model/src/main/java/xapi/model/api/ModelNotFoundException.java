/**
 *
 */
package xapi.model.api;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public class ModelNotFoundException extends Exception {

  private static final long serialVersionUID = 9198860855146141414L;
  private final ModelKey modelKey;

  public ModelNotFoundException(final ModelKey modelKey) {
    super("Could not find model " + modelKey);
    this.modelKey = modelKey;
  }

  /**
   * @return -> modelKey
   */
  public ModelKey getModelKey() {
    return modelKey;
  }

}
