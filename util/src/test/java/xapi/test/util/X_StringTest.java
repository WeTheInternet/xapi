package xapi.test.util;

import org.junit.Test;
import xapi.string.X_String;

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

  @Test
  public void testEncodeUriComponent() {
    checkEncoding("\"A\" B ± \"");
    checkEncoding("http://a+b c.html");
    checkEncoding("http://テスト++'تجربة!!測試.испытание");
  }

  private void checkEncoding(String input) {
    String encoded = X_String.encodeURIComponent(input);
    String decoded = X_String.decodeURIComponent(encoded);
    assertEquals(input, decoded);
  }
}
