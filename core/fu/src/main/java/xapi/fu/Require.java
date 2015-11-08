package xapi.fu;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 07/11/15.
 */
public interface Require extends Debuggable {

  default Require requireTrue(boolean result, Object ... debugValues) {
    if (!result) {
      fail(debugValues);
    }
    return this;
  }

  default void fail(Object[] debugValues) {
    if (debugEnabled()) {
      assert false : debug(debugValues);

    }
  }

  default Require requireFalse(boolean result, Object ... debugValues) {

    return this;
  }
  default Require requireNull(Object value, Object ... debugValues) {

    return this;
  }
  default Require requireNotNull(Object value, Object ... debugValues) {

    return this;
  }
  default Require requireEquals(Object actual, Object expected, Object ... debugValues) {

    return this;
  }
  default Require requireNotEquals(Object actual, Object expected, Object ... debugValues) {

    return this;
  }
  default Require requireSame(Object actual, Object expected, Object ... debugValues) {

    return this;
  }
  default Require requireNotSame(Object actual, Object expected, Object ... debugValues) {

    return this;
  }

}
