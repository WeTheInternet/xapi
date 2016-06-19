package xapi.fu;

import static xapi.fu.Log.allLogs;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 07/11/15.
 */
public interface Debuggable extends Coercible {

  default boolean debugEnabled() {
    return Boolean.valueOf("xapi.debug");
  }

  default String debug(Object ... values) {
    StringBuilder b = new StringBuilder();
    boolean first = true;
    for (Object value : values) {
      String toPrint = coerce(value, first);
      first = false;
      b.append(toPrint);
    }
    return b.toString();
  }

  default String coerce(Object value) {
    return coerce(value);
  }

  @Override
  default String listSeparator() {
    return ", ";
  }

  default void viewException(Object from, Throwable e) {

    allLogs(this, from, e)
        .log(from.getClass(), from, e);

  }

}
