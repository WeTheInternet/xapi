package com.google.gwt.reflect.test;

import static com.google.gwt.reflect.shared.GwtReflect.magicClass;

import com.google.gwt.reflect.client.strategy.ReflectionStrategy;
import com.google.gwt.reflect.test.cases.ReflectionCaseKeepsEverything;
import com.google.gwt.reflect.test.cases.ReflectionCaseKeepsNothing;
import com.google.gwt.reflect.test.cases.ReflectionCaseNoMagic;

@ReflectionStrategy(keepNothing=true)
@SuppressWarnings("rawtypes")
public class AbstractReflectionTest {

  protected static final Class CLASS_OBJECT = magicClass(Object.class);

  protected static final String METHOD_EQUALS = "equals";
  protected static final String METHOD_HASHCODE = "hashCode";
  protected static final String METHOD_TOSTRING = "toString";

  protected static final String PRIVATE_MEMBER = "privateCall";
  protected static final String PUBLIC_MEMBER = "publicCall";
  protected static final String OVERRIDE_FIELD = "overrideField";

  static final Class<ReflectionCaseNoMagic> NO_MAGIC = ReflectionCaseNoMagic.class;
  static final Class<ReflectionCaseNoMagic.Subclass> NO_MAGIC_SUBCLASS = ReflectionCaseNoMagic.Subclass.class;
  static final Class<ReflectionCaseKeepsEverything> KEEPS_EVERYTHING = magicClass(ReflectionCaseKeepsEverything.class);
  static final Class<ReflectionCaseKeepsNothing> KEEPS_NONE = magicClass(ReflectionCaseKeepsNothing.class);


  static public void fail(final String message) {
      if (message == null) {
          throw new AssertionError();
      }
      throw new AssertionError(message);
  }

  /**
   * Asserts that a condition is true. If it isn't it throws
   * an AssertionFailedError with the given message.
   */
  static public void assertTrue(final String message, final boolean condition) {
      if (!condition) {
          fail(message);
      }
  }

  /**
   * Asserts that a condition is true. If it isn't it throws
   * an AssertionFailedError.
   */
  static public void assertTrue(final boolean condition) {
      assertTrue(null, condition);
  }

  /**
   * Asserts that a condition is false. If it isn't it throws
   * an AssertionFailedError with the given message.
   */
  static public void assertFalse(final String message, final boolean condition) {
      assertTrue(message, !condition);
  }

  /**
   * Asserts that a condition is false. If it isn't it throws
   * an AssertionFailedError.
   */
  static public void assertFalse(final boolean condition) {
      assertFalse(null, condition);
  }

  /**
   * Asserts that an object isn't null. If it is
   * an AssertionFailedError is thrown with the given message.
   */
  static public void assertNotNull(final String message, final Object object) {
      assertTrue(message, object != null);
  }

  /**
   * Asserts that an object isn't null.
   */
  static public void assertNotNull(final Object object) {
      assertNotNull(null, object);
  }

  /**
   * Asserts that an object is null. If it isn't an {@link AssertionError} is
   * thrown.
   * Message contains: Expected: <null> but was: object
   *
   * @param object Object to check or <code>null</code>
   */
  static public void assertNull(final Object object) {
      if (object != null) {
          assertNull("Expected: <null> but was: " + object.toString(), object);
      }
  }

  /**
   * Asserts that an object is null.  If it is not
   * an AssertionFailedError is thrown with the given message.
   */
  static public void assertNull(final String message, final Object object) {
      assertTrue(message, object == null);
  }


  /**
   * Asserts that two objects are equal. If they are not
   * an AssertionFailedError is thrown with the given message.
   */
  static public void assertEquals(final String message, final Object expected, final Object actual) {
      if (expected == null && actual == null) {
          return;
      }
      if (expected != null && expected.equals(actual)) {
          return;
      }
      failNotEquals(message, expected, actual);
  }

  /**
   * Asserts that two objects are equal. If they are not
   * an AssertionFailedError is thrown.
   */
  static public void assertEquals(final Object expected, final Object actual) {
      assertEquals(null, expected, actual);
  }

  /**
   * Asserts that two Strings are equal.
   */
  static public void assertEquals(final String message, final String expected, final String actual) {
      if (expected == null && actual == null) {
          return;
      }
      if (expected != null && expected.equals(actual)) {
          return;
      }
      final String cleanMessage = message == null ? "" : message;
      throw new ComparisonFailure(cleanMessage, expected, actual);
  }

  /**
   * Asserts that two Strings are equal.
   */
  static public void assertEquals(final String expected, final String actual) {
      assertEquals(null, expected, actual);
  }

  /**
   * Asserts that two doubles are equal concerning a delta.  If they are not
   * an AssertionFailedError is thrown with the given message.  If the expected
   * value is infinity then the delta value is ignored.
   */
  static public void assertEquals(final String message, final double expected, final double actual, final double delta) {
      if (Double.compare(expected, actual) == 0) {
          return;
      }
      if (!(Math.abs(expected - actual) <= delta)) {
          failNotEquals(message, new Double(expected), new Double(actual));
      }
  }

  /**
   * Asserts that two doubles are equal concerning a delta. If the expected
   * value is infinity then the delta value is ignored.
   */
  static public void assertEquals(final double expected, final double actual, final double delta) {
      assertEquals(null, expected, actual, delta);
  }

  /**
   * Asserts that two floats are equal concerning a positive delta. If they
   * are not an AssertionFailedError is thrown with the given message. If the
   * expected value is infinity then the delta value is ignored.
   */
  static public void assertEquals(final String message, final float expected, final float actual, final float delta) {
      if (Float.compare(expected, actual) == 0) {
          return;
      }
      if (!(Math.abs(expected - actual) <= delta)) {
          failNotEquals(message, new Float(expected), new Float(actual));
      }
  }

  /**
   * Asserts that two floats are equal concerning a delta. If the expected
   * value is infinity then the delta value is ignored.
   */
  static public void assertEquals(final float expected, final float actual, final float delta) {
      assertEquals(null, expected, actual, delta);
  }

  /**
   * Asserts that two longs are equal. If they are not
   * an AssertionFailedError is thrown with the given message.
   */
  static public void assertEquals(final String message, final long expected, final long actual) {
      assertEquals(message, new Long(expected), new Long(actual));
  }

  /**
   * Asserts that two longs are equal.
   */
  static public void assertEquals(final long expected, final long actual) {
      assertEquals(null, expected, actual);
  }

  /**
   * Asserts that two booleans are equal. If they are not
   * an AssertionFailedError is thrown with the given message.
   */
  static public void assertEquals(final String message, final boolean expected, final boolean actual) {
      assertEquals(message, Boolean.valueOf(expected), Boolean.valueOf(actual));
  }

  /**
   * Asserts that two booleans are equal.
   */
  static public void assertEquals(final boolean expected, final boolean actual) {
      assertEquals(null, expected, actual);
  }

  /**
   * Asserts that two bytes are equal. If they are not
   * an AssertionFailedError is thrown with the given message.
   */
  static public void assertEquals(final String message, final byte expected, final byte actual) {
      assertEquals(message, new Byte(expected), new Byte(actual));
  }

  /**
   * Asserts that two bytes are equal.
   */
  static public void assertEquals(final byte expected, final byte actual) {
      assertEquals(null, expected, actual);
  }

  /**
   * Asserts that two chars are equal. If they are not
   * an AssertionFailedError is thrown with the given message.
   */
  static public void assertEquals(final String message, final char expected, final char actual) {
      assertEquals(message, new Character(expected), new Character(actual));
  }

  /**
   * Asserts that two chars are equal.
   */
  static public void assertEquals(final char expected, final char actual) {
      assertEquals(null, expected, actual);
  }

  /**
   * Asserts that two shorts are equal. If they are not
   * an AssertionFailedError is thrown with the given message.
   */
  static public void assertEquals(final String message, final short expected, final short actual) {
      assertEquals(message, new Short(expected), new Short(actual));
  }

  /**
   * Asserts that two shorts are equal.
   */
  static public void assertEquals(final short expected, final short actual) {
      assertEquals(null, expected, actual);
  }

  /**
   * Asserts that two ints are equal. If they are not
   * an AssertionFailedError is thrown with the given message.
   */
  static public void assertEquals(final String message, final int expected, final int actual) {
      assertEquals(message, new Integer(expected), new Integer(actual));
  }

  /**
   * Asserts that two ints are equal.
   */
  static public void assertEquals(final int expected, final int actual) {
      assertEquals(null, expected, actual);
  }

  private static boolean isEquals(final Object expected, final Object actual) {
    return expected.equals(actual);
  }

  private static boolean equalsRegardingNull(final Object expected, final Object actual) {
      if (expected == null) {
          return actual == null;
      }

      return isEquals(expected, actual);
  }

  /**
   * Asserts that two objects are <b>not</b> equals. If they are, an
   * {@link AssertionError} is thrown with the given message. If
   * <code>first</code> and <code>second</code> are <code>null</code>,
   * they are considered equal.
   *
   * @param message the identifying message for the {@link AssertionError} (<code>null</code>
   * okay)
   * @param first first value to check
   * @param second the value to check against <code>first</code>
   */
  static public void assertNotEquals(final String message, final Object first,
          final Object second) {
      if (equalsRegardingNull(first, second)) {
          failEquals(message, first);
      }
  }

  /**
   * Asserts that two objects are <b>not</b> equals. If they are, an
   * {@link AssertionError} without a message is thrown. If
   * <code>first</code> and <code>second</code> are <code>null</code>,
   * they are considered equal.
   *
   * @param first first value to check
   * @param second the value to check against <code>first</code>
   */
  static public void assertNotEquals(final Object first, final Object second) {
      assertNotEquals(null, first, second);
  }


  private static void failEquals(final String message, final Object actual) {
      String formatted = "Values should be different. ";
      if (message != null) {
          formatted = message + ". ";
      }

      formatted += "Actual: " + actual;
      fail(formatted);
  }

  /**
   * Asserts that two longs are <b>not</b> equals. If they are, an
   * {@link AssertionError} is thrown with the given message.
   *
   * @param message the identifying message for the {@link AssertionError} (<code>null</code>
   * okay)
   * @param first first value to check
   * @param second the value to check against <code>first</code>
   */
  static public void assertNotEquals(final String message, final long first, final long second) {
      assertNotEquals(message, (Long) first, (Long) second);
  }

  /**
   * Asserts that two longs are <b>not</b> equals. If they are, an
   * {@link AssertionError} without a message is thrown.
   *
   * @param first first value to check
   * @param second the value to check against <code>first</code>
   */
  static public void assertNotEquals(final long first, final long second) {
      assertNotEquals(null, first, second);
  }

  /**
   * Asserts that two doubles or floats are <b>not</b> equal to within a positive delta.
   * If they are, an {@link AssertionError} is thrown with the given
   * message. If the expected value is infinity then the delta value is
   * ignored. NaNs are considered equal:
   * <code>assertNotEquals(Double.NaN, Double.NaN, *)</code> fails
   *
   * @param message the identifying message for the {@link AssertionError} (<code>null</code>
   * okay)
   * @param first first value to check
   * @param second the value to check against <code>first</code>
   * @param delta the maximum delta between <code>expected</code> and
   * <code>actual</code> for which both numbers are still
   * considered equal.
   */
  static public void assertNotEquals(final String message, final double first,
          final double second, final double delta) {
      if (!doubleIsDifferent(first, second, delta)) {
          failEquals(message, new Double(first));
      }
  }

  /**
   * Asserts that two doubles or floats are <b>not</b> equal to within a positive delta.
   * If they are, an {@link AssertionError} is thrown. If the expected
   * value is infinity then the delta value is ignored.NaNs are considered
   * equal: <code>assertNotEquals(Double.NaN, Double.NaN, *)</code> fails
   *
   * @param first first value to check
   * @param second the value to check against <code>first</code>
   * @param delta the maximum delta between <code>expected</code> and
   * <code>actual</code> for which both numbers are still
   * considered equal.
   */
  static public void assertNotEquals(final double first, final double second, final double delta) {
      assertNotEquals(null, first, second, delta);
  }


  static private boolean doubleIsDifferent(final double d1, final double d2, final double delta) {
      if (Double.compare(d1, d2) == 0) {
          return false;
      }
      if ((Math.abs(d1 - d2) <= delta)) {
          return false;
      }

      return true;
  }

  static public void failNotEquals(final String message, final Object expected, final Object actual) {
    fail(format(message, expected, actual));
}

public static String format(final String message, final Object expected, final Object actual) {
    String formatted = "";
    if (message != null && message.length() > 0) {
        formatted = message + " ";
    }
    return formatted + "expected:<" + expected + "> but was:<" + actual + ">";
}

}


/**
 * Thrown when an {@link org.junit.Assert#assertEquals(Object, Object) assertEquals(String, String)} fails. Create and throw
 * a <code>ComparisonFailure</code> manually if you want to show users the difference between two complex
 * strings.
 *
 * Inspired by a patch from Alex Chaffee (alex@purpletech.com)
 *
 * @since 4.0
 */
class ComparisonFailure extends AssertionError {
    /**
     * The maximum length for fExpected and fActual. If it is exceeded, the strings should be shortened.
     *
     * @see ComparisonCompactor
     */
    private static final int MAX_CONTEXT_LENGTH = 20;
    private static final long serialVersionUID = 1L;

    private final String fExpected;
    private final String fActual;

    /**
     * Constructs a comparison failure.
     *
     * @param message the identifying message or null
     * @param expected the expected string value
     * @param actual the actual string value
     */
    public ComparisonFailure(final String message, final String expected, final String actual) {
        super(message);
        fExpected = expected;
        fActual = actual;
    }

    /**
     * Returns "..." in place of common prefix and "..." in
     * place of common suffix between expected and actual.
     *
     * @see Throwable#getMessage()
     */
    @Override
    public String getMessage() {
        return new ComparisonCompactor(MAX_CONTEXT_LENGTH, fExpected, fActual).compact(super.getMessage());
    }

    /**
     * Returns the actual string value
     *
     * @return the actual string value
     */
    public String getActual() {
        return fActual;
    }

    /**
     * Returns the expected string value
     *
     * @return the expected string value
     */
    public String getExpected() {
        return fExpected;
    }

    private static class ComparisonCompactor {
        private static final String ELLIPSIS = "...";
        private static final String DELTA_END = "]";
        private static final String DELTA_START = "[";

        /**
         * The maximum length for <code>expected</code> and <code>actual</code>. When <code>contextLength</code>
         * is exceeded, the Strings are shortened
         */
        private final int fContextLength;
        private final String fExpected;
        private final String fActual;
        private int fPrefix;
        private int fSuffix;

        /**
         * @param contextLength the maximum length for <code>expected</code> and <code>actual</code>. When contextLength
         * is exceeded, the Strings are shortened
         * @param expected the expected string value
         * @param actual the actual string value
         */
        public ComparisonCompactor(final int contextLength, final String expected, final String actual) {
            fContextLength = contextLength;
            fExpected = expected;
            fActual = actual;
        }

        private String compact(final String message) {
            if (fExpected == null || fActual == null || areStringsEqual()) {
                return AbstractReflectionTest.format(message, fExpected, fActual);
            }

            findCommonPrefix();
            findCommonSuffix();
            final String expected = compactString(fExpected);
            final String actual = compactString(fActual);
            return AbstractReflectionTest.format(message, expected, actual);
        }

        private String compactString(final String source) {
            String result = DELTA_START + source.substring(fPrefix, source.length() - fSuffix + 1) + DELTA_END;
            if (fPrefix > 0) {
                result = computeCommonPrefix() + result;
            }
            if (fSuffix > 0) {
                result = result + computeCommonSuffix();
            }
            return result;
        }

        private void findCommonPrefix() {
            fPrefix = 0;
            final int end = Math.min(fExpected.length(), fActual.length());
            for (; fPrefix < end; fPrefix++) {
                if (fExpected.charAt(fPrefix) != fActual.charAt(fPrefix)) {
                    break;
                }
            }
        }

        private void findCommonSuffix() {
            int expectedSuffix = fExpected.length() - 1;
            int actualSuffix = fActual.length() - 1;
            for (; actualSuffix >= fPrefix && expectedSuffix >= fPrefix; actualSuffix--, expectedSuffix--) {
                if (fExpected.charAt(expectedSuffix) != fActual.charAt(actualSuffix)) {
                    break;
                }
            }
            fSuffix = fExpected.length() - expectedSuffix;
        }

        private String computeCommonPrefix() {
            return (fPrefix > fContextLength ? ELLIPSIS : "") + fExpected.substring(Math.max(0, fPrefix - fContextLength), fPrefix);
        }

        private String computeCommonSuffix() {
            final int end = Math.min(fExpected.length() - fSuffix + 1 + fContextLength, fExpected.length());
            return fExpected.substring(fExpected.length() - fSuffix + 1, end) + (fExpected.length() - fSuffix + 1 < fExpected.length() - fContextLength ? ELLIPSIS : "");
        }

        private boolean areStringsEqual() {
            return fExpected.equals(fActual);
        }
    }
}