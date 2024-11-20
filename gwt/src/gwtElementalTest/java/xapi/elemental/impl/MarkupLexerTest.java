package xapi.elemental.impl;

import org.junit.Assert;
import org.junit.Test;

public class MarkupLexerTest {

  private static final String HELLO_WORLD = "Hello World";
  private static final String HELLO_WORLD_LINK = "<a "
      + "href=\"/"+HELLO_WORLD.replace(' ', '-')+"\" >"
      + HELLO_WORLD+ "</a>";

  @Test
  public void testLinkFormat_OnlyLink() {
    final LexerForMarkup lex = new LexerForMarkup();
    lex.lex("#["+HELLO_WORLD+ "]");
    Assert.assertEquals(HELLO_WORLD_LINK, lex.toSource());
  }

  @Test
  public void testLinkFormat_WhitespaceAfterLink() {
    final LexerForMarkup lex = new LexerForMarkup();
    lex.lex("  #["+HELLO_WORLD+ "]  ");
    Assert.assertEquals("  "+HELLO_WORLD_LINK+"  ", lex.toSource());
  }

  @Test
  public void testLinkFormat_WhitespaceBeforeLink() {
    final LexerForMarkup lex = new LexerForMarkup();
    lex.lex("  #["+HELLO_WORLD+ "]");
    Assert.assertEquals("  "+HELLO_WORLD_LINK, lex.toSource());
  }

  @Test
  public void testLinkFormat_WordAfterLink() {
    final LexerForMarkup lex = new LexerForMarkup();
    lex.lex(" Word #["+HELLO_WORLD+ "]After");
    Assert.assertEquals(" Word "+HELLO_WORLD_LINK+"After", lex.toSource());
  }

  @Test
  public void testLinkFormat_WordBeforeLink() {
    final LexerForMarkup lex = new LexerForMarkup();
    lex.lex(" Word #["+HELLO_WORLD+ "]");
    Assert.assertEquals(" Word "+HELLO_WORLD_LINK, lex.toSource());
  }
}
