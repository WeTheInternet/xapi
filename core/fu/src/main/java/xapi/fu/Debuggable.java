package xapi.fu;

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
    for (Object value : values) {
      String toPrint = coerce(value);
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

  default void viewException(Throwable e) {
    if (this instanceof Log) {
      ((Log)this).log(getClass(), e);
    } else {
      e.printStackTrace();
    }
  }

}
