package xapi.fu;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 07/11/15.
 */
public interface Debuggable {

  default boolean debugEnabled() {
    return Boolean.valueOf("xapi.debug");
  }

  default String debug(Object ... values) {
    StringBuilder b = new StringBuilder();
    for (Object value : values) {
      String toPrint = coerce(value);
      b.append(toPrint);
    }
    return b.toString();
  }

  default String coerce(Object value) {
    return Fu.jutsu.coerce(value);
  };

}
