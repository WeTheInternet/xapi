package xapi.ui.api;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/18/16.
 */
public enum ElementPosition {
  BEFORE_BEGIN("beforebegin"),
  AFTER_BEGIN("afterbegin"),
  BEFORE_END("beforeend"),
  AFTER_END("afterend");
  private final String position;

  ElementPosition(String position) {this.position = position;}

  public String position() {
    return position;
  }
}
