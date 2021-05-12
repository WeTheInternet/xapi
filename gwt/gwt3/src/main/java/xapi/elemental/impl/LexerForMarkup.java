package xapi.elemental.impl;

import static xapi.util.X_Util.firstNotNull;

import xapi.source.lex.CharIterator;
import xapi.source.lex.Lexer;
import xapi.source.lex.LexerDefault;
import xapi.source.lex.LexerStack;
import xapi.source.lex.StringCharIterator;

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
    return onWord(this, "<a "
      + "href=\""+formatLinkHref(linkText)+"\" "
      + firstNotNull(commonLinkAttributes(),"")
      +">"+formatLinkText(linkText)+"</a>", str);
  }

  protected String commonLinkAttributes() {
    return linkAttributes;
  }

  protected String formatLinkText(String linkText) {
    return linkText;
  }

  protected String formatLinkHref(String linkText) {
    return "/"+linkText.replace(' ', '-');
  }

  @Override
  protected Lexer onWord(LexerStack parent, String word, CharIterator str) {
    b.append(word);
    return super.onWord(parent, word, str);
  }


  @Override
  protected Lexer onWhitespace(char c, CharIterator str) {
    if (c == '\n') {
      b.append("<br/>");
      if (str.hasNext() && isWhitespace(str.peek())) {
        // A newline followed by whitespace is considered a new paragraph
      }
      return super.onWhitespace(c, str);
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

  @Override
  public void clear() {
    b.setLength(0);
  }

}
