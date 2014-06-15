package xapi.elemental.impl;

import org.junit.Assert;
import org.junit.Test;

public class MarkupLexerTest {

  private static final String HELLO_WORLD = "Hello World";
  private static final String HELLO_WORLD_LINK = "<a "
      + "href=\"/"+HELLO_WORLD+"\" >"
      + HELLO_WORLD+ "</a>";

  @Test
  public void testLinkFormat_OnlyLink() {
    LexerForMarkup lex = new LexerForMarkup();
    lex.lex("#["+HELLO_WORLD+ "]");
    Assert.assertEquals(HELLO_WORLD_LINK, lex.toSource());
  }

  @Test
  public void testLinkFormat_WhitespaceBeforeLink() {
    LexerForMarkup lex = new LexerForMarkup();
    lex.lex("  #["+HELLO_WORLD+ "]");
    Assert.assertEquals("  "+HELLO_WORLD_LINK, lex.toSource());
  }

  @Test
  public void testLinkFormat_WordBeforeLink() {
    LexerForMarkup lex = new LexerForMarkup();
    lex.lex(" Word #["+HELLO_WORLD+ "]");
    Assert.assertEquals(" Word "+HELLO_WORLD_LINK, lex.toSource());
  }

  @Test
  public void testLinkFormat_WhitespaceAfterLink() {
    LexerForMarkup lex = new LexerForMarkup();
    lex.lex("  #["+HELLO_WORLD+ "]  ");
    Assert.assertEquals("  "+HELLO_WORLD_LINK+"  ", lex.toSource());
  }

  @Test
  public void testLinkFormat_WordAfterLink() {
    LexerForMarkup lex = new LexerForMarkup();
    lex.lex(" Word #["+HELLO_WORLD+ "]After");
    Assert.assertEquals(" Word "+HELLO_WORLD_LINK+"After", lex.toSource());
  }
}
