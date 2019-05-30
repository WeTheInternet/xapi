package xapi.test;

import xapi.fu.Filter.Filter1Unsafe;

/**
 * Some basic assertion utilities; based on org.junit.Assert,
 * but without gwt-unfriendly capabilities, like the use of java.lang.reflect.Array.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public class Assert {

  /**
   * Protect constructor since it is a static only class
   */
  protected Assert() {
  }

  /**
   * Asserts that a condition is true. If it isn't it throws an
   * {@link AssertionError} with the given message.
   *
   * @param message
   *            the identifying message for the {@link AssertionError} (<code>null</code>
   *            okay)
   * @param condition
   *            condition to be checked
   */
  static public void assertTrue(String message, boolean condition) {
    if (!condition)
      fail(message);
  }

  /**
   * Asserts that a condition is true. If it isn't it throws an
   * {@link AssertionError} without a message.
   *
   * @param condition
   *            condition to be checked
   */
  static public void assertTrue(boolean condition) {
    assertTrue(null, condition);
  }

  /**
   * Asserts that a condition is false. If it isn't it throws an
   * {@link AssertionError} with the given message.
   *
   * @param message
   *            the identifying message for the {@link AssertionError} (<code>null</code>
   *            okay)
   * @param condition
   *            condition to be checked
   */
  static public void assertFalse(String message, boolean condition) {
    assertTrue(message, !condition);
  }

  /**
   * Asserts that a condition is false. If it isn't it throws an
   * {@link AssertionError} without a message.
   *
   * @param condition
   *            condition to be checked
   */
  static public void assertFalse(boolean condition) {
    assertFalse(null, condition);
  }

  /**
   * Fails a test with the given message.
   *
   * @param message
   *            the identifying message for the {@link AssertionError} (<code>null</code>
   *            okay)
   * @see AssertionError
   */
  static public void fail(String message) {
    throw new AssertionError(message == null ? "" : message);
  }

  /**
   * Fails a test with no message.
   */
  static public void fail() {
    fail(null);
  }

  /**
   * Asserts that two objects are equal. If they are not, an
   * {@link AssertionError} is thrown with the given message. If
   * <code>expected</code> and <code>actual</code> are <code>null</code>,
   * they are considered equal.
   *
   * @param message
   *            the identifying message for the {@link AssertionError} (<code>null</code>
   *            okay)
   * @param expected
   *            expected value
   * @param actual
   *            actual value
   */
  static public void assertEquals(String message, Object expected,
      Object actual) {
    if (expected == null && actual == null)
      return;
    if (expected != null && isEquals(expected, actual))
      return;
    else if (expected instanceof String && actual instanceof String) {
      String cleanMessage= message == null ? "" : message;
      throw asssertionError(cleanMessage, expected, actual);
    } else
      failNotEquals(message, expected, actual);
  }

  private static AssertionError asssertionError(String cleanMessage, Object expected, Object actual) {
    return new AssertionError("Expected: "+ expected +"\nReceived:"+
        actual+"\n["+cleanMessage+"]");
  }

  private static boolean isEquals(Object expected, Object actual) {
    return expected.equals(actual);
  }

  /**
   * Asserts that two objects are equal. If they are not, an
   * {@link AssertionError} without a message is thrown. If
   * <code>expected</code> and <code>actual</code> are <code>null</code>,
   * they are considered equal.
   *
   * @param expected
   *            expected value
   * @param actual
   *            the value to check against <code>expected</code>
   */
  static public void assertEquals(Object expected, Object actual) {
    assertEquals(null, expected, actual);
  }

  /**
   * Asserts that two object arrays are equal. If they are not, an
   * {@link AssertionError} is thrown with the given message. If
   * <code>expected</code> and <code>actual</code> are <code>null</code>,
   * they are considered equal.
   *
   * @param message
   *            the identifying message for the {@link AssertionError} (<code>null</code>
   *            okay)
   * @param expected
   *            Object array or array of arrays (multi-dimensional array) with
   *            expected values.
   * @param actual
   *            Object array or array of arrays (multi-dimensional array) with
   *            actual values
   */
  public static void assertArrayEquals(String message, Object[] expected,
      Object[] actual) throws AssertionError {
    internalArrayEquals(message, expected, actual);
  }

  /**
   * Asserts that two object arrays are equal. If they are not, an
   * {@link AssertionError} is thrown. If <code>expected</code> and
   * <code>actual</code> are <code>null</code>, they are considered
   * equal.
   *
   * @param expected
   *            Object array or array of arrays (multi-dimensional array) with
   *            expected values
   * @param actual
   *            Object array or array of arrays (multi-dimensional array) with
   *            actual values
   */
  public static void assertArrayEquals(Object[] expected, Object[] actual) {
    assertArrayEquals(null, expected, actual);
  }

  /**
   * Asserts that two byte arrays are equal. If they are not, an
   * {@link AssertionError} is thrown with the given message.
   *
   * @param message
   *            the identifying message for the {@link AssertionError} (<code>null</code>
   *            okay)
   * @param expected
   *            byte array with expected values.
   * @param actual
   *            byte array with actual values
   */
  public static void assertArrayEquals(String message, byte[] expected,
      byte[] actual) throws AssertionError {
    if (expected.length != actual.length) {
      throw new AssertionError("Expected array length: "+expected.length+"\n" +
          "Received: "+actual.length+(message==null?"":"\n"+message));
    }
    for (int i = expected.length; i-->0;) {
      if (expected[i] != actual[i]) {
        throw newArrayFail(i, expected[i], actual[i]);
      }
    }
  }

  /**
   * Asserts that two byte arrays are equal. If they are not, an
   * {@link AssertionError} is thrown.
   *
   * @param expected
   *            byte array with expected values.
   * @param actual
   *            byte array with actual values
   */
  public static void assertArrayEquals(byte[] expected, byte[] actual) {
    assertArrayEquals(null, expected, actual);
  }

  /**
   * Asserts that two char arrays are equal. If they are not, an
   * {@link AssertionError} is thrown with the given message.
   *
   * @param message
   *            the identifying message for the {@link AssertionError} (<code>null</code>
   *            okay)
   * @param expected
   *            char array with expected values.
   * @param actual
   *            char array with actual values
   */
  public static void assertArrayEquals(String message, char[] expected,
      char[] actual) throws AssertionError {
    if (expected.length != actual.length) {
      throw new AssertionError("Expected array length: "+expected.length+"\n" +
          "Received: "+actual.length+(message==null?"":"\n"+message));
    }
    for (int i = expected.length; i-->0;) {
      if (expected[i] != actual[i]) {
        throw newArrayFail(i, expected[i], actual[i]);
      }
    }
  }

  /**
   * Asserts that two char arrays are equal. If they are not, an
   * {@link AssertionError} is thrown.
   *
   * @param expected
   *            char array with expected values.
   * @param actual
   *            char array with actual values
   */
  public static void assertArrayEquals(char[] expected, char[] actual) {
    assertArrayEquals(null, expected, actual);
  }

  /**
   * Asserts that two short arrays are equal. If they are not, an
   * {@link AssertionError} is thrown with the given message.
   *
   * @param message
   *            the identifying message for the {@link AssertionError} (<code>null</code>
   *            okay)
   * @param expected
   *            short array with expected values.
   * @param actual
   *            short array with actual values
   */
  public static void assertArrayEquals(String message, short[] expected,
      short[] actual) throws AssertionError {
    if (expected.length != actual.length) {
      throw new AssertionError("Expected array length: "+expected.length+"\n" +
          "Received: "+actual.length+(message==null?"":"\n"+message));
    }
    for (int i = expected.length; i-->0;) {
      if (expected[i] != actual[i]) {
        throw newArrayFail(i, expected[i], actual[i]);
      }
    }
  }

  /**
   * Asserts that two short arrays are equal. If they are not, an
   * {@link AssertionError} is thrown.
   *
   * @param expected
   *            short array with expected values.
   * @param actual
   *            short array with actual values
   */
  public static void assertArrayEquals(short[] expected, short[] actual) {
    assertArrayEquals(null, expected, actual);
  }

  /**
   * Asserts that two int arrays are equal. If they are not, an
   * {@link AssertionError} is thrown with the given message.
   *
   * @param message
   *            the identifying message for the {@link AssertionError} (<code>null</code>
   *            okay)
   * @param expected
   *            int array with expected values.
   * @param actual
   *            int array with actual values
   */
  public static void assertArrayEquals(String message, int[] expected,
      int[] actual) throws AssertionError {
    if (expected.length != actual.length) {
      throw new AssertionError("Expected array length: "+expected.length+"\n" +
          "Received: "+actual.length+(message==null?"":"\n"+message));
    }
    for (int i = expected.length; i-->0;) {
      if (expected[i] != actual[i]) {
        throw newArrayFail(i, expected[i], actual[i]);
      }
    }
  }

  /**
   * Asserts that two int arrays are equal. If they are not, an
   * {@link AssertionError} is thrown.
   *
   * @param expected
   *            int array with expected values.
   * @param actual
   *            int array with actual values
   */
  public static void assertArrayEquals(int[] expected, int[] actual) {
    assertArrayEquals(null, expected, actual);
  }

  /**
   * Asserts that two long arrays are equal. If they are not, an
   * {@link AssertionError} is thrown with the given message.
   *
   * @param message
   *            the identifying message for the {@link AssertionError} (<code>null</code>
   *            okay)
   * @param expected
   *            long array with expected values.
   * @param actual
   *            long array with actual values
   */
  public static void assertArrayEquals(String message, long[] expected,
      long[] actual) throws AssertionError {
    if (expected.length != actual.length) {
      throw new AssertionError("Expected array length: "+expected.length+"\n" +
          "Received: "+actual.length+(message==null?"":"\n"+message));
    }
    for (int i = expected.length; i-->0;) {
      if (expected[i] != actual[i]) {
        throw newArrayFail(i, expected[i], actual[i]);
      }
    }
  }

  /**
   * Asserts that two long arrays are equal. If they are not, an
   * {@link AssertionError} is thrown.
   *
   * @param expected
   *            long array with expected values.
   * @param actual
   *            long array with actual values
   */
  public static void assertArrayEquals(long[] expected, long[] actual) {
    assertArrayEquals(null, expected, actual);
  }

  /**
   * Asserts that two double arrays are equal. If they are not, an
   * {@link AssertionError} is thrown.
   *
   * @param expected
   *            double array with expected values.
   * @param actual
   *            double array with actual values
   */
  public static void assertArrayEquals(double[] expected, double[] actual) {
    assertArrayEquals(null, expected, actual);
  }

  /**
   * Asserts that two double arrays are equal. If they are not, an
   * {@link AssertionError} is thrown.
   * @param message
   *        debug message
   * @param expected
   *            double array with expected values.
   * @param actual
   *            double array with actual values
   */
  public static void assertArrayEquals(String message, double[] expected, double[] actual) {
    if (expected.length != actual.length) {
      throw new AssertionError("Expected array length: "+expected.length+"\n" +
          "Received: "+actual.length+(message==null?"":"\n"+message));
    }
    for (int i = expected.length; i-->0;) {
      double delta = expected[i] - actual[i];
      if (Math.abs(delta) > 0.00000000000001) {
        throw newArrayFail(i, expected[i], actual[i]);
      }
    }
  }

  /**
   * Asserts that two float arrays are equal. If they are not, an
   * {@link AssertionError} is thrown.
   *
   * @param expected
   *            float array with expected values.
   * @param actual
   *            float array with actual values
   */
  public static void assertArrayEquals(float[] expected, float[] actual) {
    assertArrayEquals(null, expected, actual);
  }

  /**
   * Asserts that two float arrays are equal. If they are not, an
   * {@link AssertionError} is thrown.
   * @param message
   *            debug message
   * @param expected
   *            float array with expected values.
   * @param actual
   *            float array with actual values
   */
  public static void assertArrayEquals(String message, float[] expected, float[] actual) {
    if (expected.length != actual.length) {
      throw new AssertionError("Expected array length: "+expected.length+"\n" +
          "Received: "+actual.length+(message==null?"":"\n"+message));
    }
    for (int i = expected.length; i-->0;) {
      float delta = expected[i] - actual[i];
      if (Math.abs(delta) > 0.0000000001f) {
        throw newArrayFail(i, expected[i], actual[i]);
      }
    }
  }

  /**
   * Asserts that two object arrays are equal. If they are not, an
   * {@link AssertionError} is thrown with the given message. If
   * <code>expected</code> and <code>actual</code> are <code>null</code>,
   * they are considered equal.
   *
   * @param message
   *            the identifying message for the {@link AssertionError} (<code>null</code>
   *            okay)
   * @param expected
   *            Object array or array of arrays (multi-dimensional array) with
   *            expected values.
   * @param actual
   *            Object array or array of arrays (multi-dimensional array) with
   *            actual values
   */
  private static <T> void  internalArrayEquals(String message, T[] expected,
      T[] actual) throws AssertionError {
    if (expected.length != actual.length) {
      throw new AssertionError("Expected array length: "+expected.length+"\n" +
      		"Received: "+actual.length+(message==null?"":"\n"+message));
    }
    for (int i = expected.length; i-->0;) {
      if (expected[i] == null) {
        if (actual[i] != null) {
          throw newArrayFail(i, expected[i], actual[i]);
        }
      } else {
        if (!expected[i].equals(actual[i])) {
          throw newArrayFail(i, expected[i], actual[i]);
        }
      }
    }
  }

  private static AssertionError newArrayFail(int index, Object expected, Object actual) {
    return new AssertionError("Array comparison failure at index " +
      index+"\n" +
      "Expected " +expected+ ", " +
      "got: "+actual);
  }

  /**
   * Asserts that two doubles or floats are equal to within a positive delta.
   * If they are not, an {@link AssertionError} is thrown with the given
   * message. If the expected value is infinity then the delta value is
   * ignored. NaNs are considered equal:
   * <code>assertEquals(Double.NaN, Double.NaN, *)</code> passes
   *
   * @param message
   *            the identifying message for the {@link AssertionError} (<code>null</code>
   *            okay)
   * @param expected
   *            expected value
   * @param actual
   *            the value to check against <code>expected</code>
   * @param delta
   *            the maximum delta between <code>expected</code> and
   *            <code>actual</code> for which both numbers are still
   *            considered equal.
   */
  static public void assertEquals(String message, double expected,
      double actual, double delta) {
    if (Double.compare(expected, actual) == 0)
      return;
    if (!(Math.abs(expected - actual) <= delta))
      failNotEquals(message, new Double(expected), new Double(actual));
  }

  /**
   * Asserts that two longs are equal. If they are not, an
   * {@link AssertionError} is thrown.
   *
   * @param expected
   *            expected long value.
   * @param actual
   *            actual long value
   */
  static public void assertEquals(long expected, long actual) {
    assertEquals(null, expected, actual);
  }

  /**
   * Asserts that two longs are equal. If they are not, an
   * {@link AssertionError} is thrown with the given message.
   *
   * @param message
   *            the identifying message for the {@link AssertionError} (<code>null</code>
   *            okay)
   * @param expected
   *            long expected value.
   * @param actual
   *            long actual value
   */
  static public void assertEquals(String message, long expected, long actual) {
    assertEquals(message, (Long) expected, (Long) actual);
  }

  /**
   * @deprecated Use
   *             <code>assertEquals(double expected, double actual, double epsilon)</code>
   *             instead
   */
  @Deprecated
  static public void assertEquals(double expected, double actual) {
    assertEquals(null, expected, actual);
  }

  /**
   * @deprecated Use
   *             <code>assertEquals(String message, double expected, double actual, double epsilon)</code>
   *             instead
   */
  @Deprecated
  static public void assertEquals(String message, double expected,
      double actual) {
    fail("Use assertEquals(expected, actual, delta) to compare floating-point numbers");
  }

  /**
   * Asserts that two doubles or floats are equal to within a positive delta.
   * If they are not, an {@link AssertionError} is thrown. If the expected
   * value is infinity then the delta value is ignored.NaNs are considered
   * equal: <code>assertEquals(Double.NaN, Double.NaN, *)</code> passes
   *
   * @param expected
   *            expected value
   * @param actual
   *            the value to check against <code>expected</code>
   * @param delta
   *            the maximum delta between <code>expected</code> and
   *            <code>actual</code> for which both numbers are still
   *            considered equal.
   */
  static public void assertEquals(double expected, double actual, double delta) {
    assertEquals(null, expected, actual, delta);
  }

  /**
   * Asserts that an object isn't null. If it is an {@link AssertionError} is
   * thrown with the given message.
   *
   * @param message
   *            the identifying message for the {@link AssertionError} (<code>null</code>
   *            okay)
   * @param object
   *            Object to check or <code>null</code>
   */
  static public void assertNotNull(String message, Object object) {
    assertTrue(message, object != null);
  }

  /**
   * Asserts that an object isn't null. If it is an {@link AssertionError} is
   * thrown.
   *
   * @param object
   *            Object to check or <code>null</code>
   */
  static public void assertNotNull(Object object) {
    assertNotNull(null, object);
  }

  /**
   * Asserts that an object is null. If it is not, an {@link AssertionError}
   * is thrown with the given message.
   *
   * @param message
   *            the identifying message for the {@link AssertionError} (<code>null</code>
   *            okay)
   * @param object
   *            Object to check or <code>null</code>
   */
  static public void assertNull(String message, Object object) {
    assertTrue(message, object == null);
  }

  /**
   * Asserts that an object is null. If it isn't an {@link AssertionError} is
   * thrown.
   *
   * @param object
   *            Object to check or <code>null</code>
   */
  static public void assertNull(Object object) {
    assertNull(null, object);
  }

  /**
   * Asserts that two objects refer to the same object. If they are not, an
   * {@link AssertionError} is thrown with the given message.
   *
   * @param message
   *            the identifying message for the {@link AssertionError} (<code>null</code>
   *            okay)
   * @param expected
   *            the expected object
   * @param actual
   *            the object to compare to <code>expected</code>
   */
  static public void assertSame(String message, Object expected, Object actual) {
    if (expected == actual)
      return;
    failNotSame(message, expected, actual);
  }

  /**
   * Asserts that two objects refer to the same object. If they are not the
   * same, an {@link AssertionError} without a message is thrown.
   *
   * @param expected
   *            the expected object
   * @param actual
   *            the object to compare to <code>expected</code>
   */
  static public void assertSame(Object expected, Object actual) {
    assertSame(null, expected, actual);
  }

  /**
   * Asserts that two objects do not refer to the same object. If they do
   * refer to the same object, an {@link AssertionError} is thrown with the
   * given message.
   *
   * @param message
   *            the identifying message for the {@link AssertionError} (<code>null</code>
   *            okay)
   * @param unexpected
   *            the object you don't expect
   * @param actual
   *            the object to compare to <code>unexpected</code>
   */
  static public void assertNotSame(String message, Object unexpected,
      Object actual) {
    if (unexpected == actual)
      failSame(message);
  }

  /**
   * Asserts that two objects do not refer to the same object. If they do
   * refer to the same object, an {@link AssertionError} without a message is
   * thrown.
   *
   * @param unexpected
   *            the object you don't expect
   * @param actual
   *            the object to compare to <code>unexpected</code>
   */
  static public void assertNotSame(Object unexpected, Object actual) {
    assertNotSame(null, unexpected, actual);
  }

  static private void failSame(String message) {
    String formatted= "";
    if (message != null)
      formatted= message + " ";
    fail(formatted + "expected not same");
  }

  static private void failNotSame(String message, Object expected,
      Object actual) {
    String formatted= "";
    if (message != null)
      formatted= message + " ";
    fail(formatted + "expected same:<" + expected + "> was not:<" + actual
        + ">");
  }

  static private void failNotEquals(String message, Object expected,
      Object actual) {
    fail(format(message, expected, actual));
  }

  static String format(String message, Object expected, Object actual) {
    String formatted= "";
    if (message != null && !message.equals(""))
      formatted= message + " ";
    String expectedtring= String.valueOf(expected);
    String actualtring= String.valueOf(actual);
    if (expectedtring.equals(actualtring))
      return formatted + "expected: "
          + formatClassAndValue(expected, expectedtring)
          + " but was: " + formatClassAndValue(actual, actualtring);
    else
      return formatted + "expected:<" + expectedtring + "> but was:<"
          + actualtring + ">";
  }

  private static String formatClassAndValue(Object value, String valueString) {
    String className= value == null ? "null" : value.getClass().getName();
    return className + "<" + valueString + ">";
  }

  /**
   * Asserts that two object arrays are equal. If they are not, an
   * {@link AssertionError} is thrown with the given message. If
   * <code>expected</code> and <code>actual</code> are <code>null</code>,
   * they are considered equal.
   *
   * @param message
   *            the identifying message for the {@link AssertionError} (<code>null</code>
   *            okay)
   * @param expected
   *            Object array or array of arrays (multi-dimensional array) with
   *            expected values.
   * @param actual
   *            Object array or array of arrays (multi-dimensional array) with
   *            actual values
   * @deprecated use assertArrayEquals
   */
  @Deprecated
  public static void assertEquals(String message, Object[] expected,
      Object[] actual) {
    assertArrayEquals(message, expected, actual);
  }

  /**
   * Asserts that two object arrays are equal. If they are not, an
   * {@link AssertionError} is thrown. If <code>expected</code> and
   * <code>actual</code> are <code>null</code>, they are considered
   * equal.
   *
   * @param expected
   *            Object array or array of arrays (multi-dimensional array) with
   *            expected values
   * @param actual
   *            Object array or array of arrays (multi-dimensional array) with
   *            actual values
   * @deprecated use assertArrayEquals
   */
  @Deprecated
  public static void assertEquals(Object[] expected, Object[] actual) {
    assertArrayEquals(expected, actual);
  }

  /**
   * Asserts that <code>actual</code> satisfies the condition specified by
   * <code>matcher</code>. If not, an {@link AssertionError} is thrown with
   * information about the matcher and failing value. Example:
   *
   * <pre>
   *   assertThat(0, is(1)); // fails:
   *     // failure message:
   *     // expected: is &lt;1&gt;
   *     // got value: &lt;0&gt;
   *   assertThat(0, is(not(1))) // passes
   * </pre>
   *
   * @param <T>
   *            the static type accepted by the matcher (this can flag obvious
   *            compile-time problems such as {@code assertThat(1, is("a"))}
   * @param actual
   *            the computed value being compared
   * @param matcher
   *            an expression, built of {@link org.hamcrest.Matcher}s, specifying allowed
   *            values
   *
   * @see org.hamcrest.CoreMatchers
   * @see org.junit.matchers.JUnitMatchers
   */
  public static <T> void assertThat(T actual, Filter1Unsafe<T> matcher) {
    assertThat("", actual, matcher);
  }

  /**
   * Asserts that <code>actual</code> satisfies the condition specified by
   * <code>matcher</code>. If not, an {@link AssertionError} is thrown with
   * the reason and information about the matcher and failing value. Example:
   *
   * <pre>
   * :
   *   assertThat(&quot;Help! Integers don't work&quot;, 0, is(1)); // fails:
   *     // failure message:
   *     // Help! Integers don't work
   *     // expected: is &lt;1&gt;
   *     // got value: &lt;0&gt;
   *   assertThat(&quot;Zero is one&quot;, 0, is(not(1))) // passes
   * </pre>
   *
   * @param reason
   *            additional information about the error
   * @param <T>
   *            the static type accepted by the matcher (this can flag obvious
   *            compile-time problems such as {@code assertThat(1, is("a"))}
   * @param actual
   *            the computed value being compared
   * @param matcher
   *            an expression, built of {@link org.hamcrest.Matcher}s, specifying allowed
   *            values
   *
   * @see org.hamcrest.CoreMatchers
   * @see org.junit.matchers.JUnitMatchers
   */
  public static <T> void assertThat(String reason, T actual,
      Filter1Unsafe<T> matcher) {
    if (!matcher.filter1(actual)) {
      throw new java.lang.AssertionError("Item "+actual+" does not match "+matcher
        +".  "+(reason == null ? "" : "\n["+reason+"]"));
    }
  }

}
