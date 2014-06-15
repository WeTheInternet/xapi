package xapi.elemental.impl;

import xapi.source.api.CharIterator;
import xapi.source.api.Lexer;
import xapi.source.impl.LexerDefault;
import xapi.source.impl.LexerStack;
import xapi.source.impl.StringCharIterator;

public class LexerForMarkup extends LexerStack {

  private StringBuilder b = new StringBuilder();
  private String linkAttributes = "";

  @Override
  protected Lexer onWordStart(char c, CharIterator str) {
    switch (c) {
      case '#':
        if (str.hasNext() && str.peek() == '[') {
          str.next();
          // Starting a #[Link].  Translate % encoded [ and ] chars
          // [ = %5B
          // ] = %5D
          StringBuilder b = new StringBuilder();
          while (str.hasNext()) {
            if (str.peek() == ']') {
              str.next();
              return onLink(b.toString(), str);
            }
            b.append(str.next());
          }
          return onWord(this, "#[", new StringCharIterator(b.toString()));
        }
      case '@':
        if (str.hasNext() && !isWhitespace(str.peek())) {
          // An @Annotation to process
        }
      // TODO handle < and >
    }
    return onWord(this, extractWord(c, str), str);
  }

  private Lexer onLink(String linkText, CharIterator str) {
    return onWord(this, "<a href=\""+formatLinkHref(linkText)+"\" "
      + commonLinkAttributes()+">"+formatLinkText(linkText)+"</a>", str);
  }

  protected String commonLinkAttributes() {
    return null;//linkAttributes;
  }

  protected String formatLinkText(String linkText) {
    return linkText;
  }

  protected String formatLinkHref(String linkText) {
    return "/"+linkText;
  }

  @Override
  protected Lexer onWord(LexerStack parent, String word, CharIterator str) {
    b.append(word);
    return super.onWord(parent, word, str);
  }


  @Override
  protected Lexer onWhitespace(char c, CharIterator str) {
    if (c == '\n') {
      onWord("<br/>", str);
      if (str.hasNext() && isWhitespace(str.peek())) {
        // A newline followed by whitespace is considered a new paragraph
      }
    }
    b.append(c);
    return super.onWhitespace(c, str);
  }

  @Override
  public String toString() {
    return toSource();
  }

  public String toSource() {
    return b.toString();
  }

  /**
   * @return the linkAttributes
   */
  public String getLinkAttributes() {
    return linkAttributes;
  }

  /**
   * @param linkAttributes the linkAttributes to set
   * @return
   */
  public LexerForMarkup setLinkAttributes(String linkAttributes) {
    assert linkAttributes != null;
    this.linkAttributes = linkAttributes;
    return this;
  }

  @Override
  public LexerForMarkup lex(String in) {
    super.lex(in);
    return this;
  }

}
