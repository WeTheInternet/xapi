package xapi.test.util;

import org.junit.Test;
import xapi.util.X_String;

import static org.junit.Assert.assertEquals;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 23/11/15.
 */
public class X_StringTest {

  @Test
  public void testNewlineNormalize() {
    assertEquals("test\newlines", X_String.normalizeNewlines("test\newlines"));
    assertEquals("test\newlines", X_String.normalizeNewlines("test\rewlines"));
    assertEquals("test\newlines", X_String.normalizeNewlines("test\r\newlines"));
  }
}
