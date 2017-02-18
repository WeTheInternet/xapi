/**
 *
 */
package xapi.elemental.impl;

import elemental.client.Browser;
import elemental.dom.Element;
import elemental.html.TextAreaElement;
import xapi.annotation.inject.SingletonDefault;
import xapi.elemental.api.ElementalService;
import xapi.elemental.api.PotentialNode;
import xapi.source.api.Lexer;
import xapi.ui.html.X_Html;
import xapi.ui.html.impl.StyleServiceDefault;
import xapi.util.api.ConvertsValue;

import com.google.gwt.core.shared.GwtIncompatible;

/**
 * @author "James X. Nelson (james@wetheinter.net)"
 */
@SingletonDefault(implFor=ElementalService.class)
public class ElementalServiceDefault
extends StyleServiceDefault
implements ElementalService {

  private Lexer lexer;
  private static final TextAreaElement escaper = Browser.getDocument().createTextAreaElement();
  @Override
  public <T, E extends Element> E toElement(Class<? super T> cls, T obj) {
    return this.<T, E>toElementBuilder(cls).convert(obj).getElement();
  }

  @Override
  public <T, E extends Element> E toElement(Class<? super T> cls, Class<?> template, T obj) {
    return this.<T, E>toElementBuilder(cls, template).convert(obj).getElement();
  }

  public <T, E extends Element> ConvertsValue<T, PotentialNode<E>> toElementBuilder(final Class<? super T> cls) {
    return toElementBuilder(cls, cls);
  }

  static class UnsupportedConverter <T, E> implements ConvertsValue<T, E> {

    @Override
    public E convert(T from) {
      throw new UnsupportedOperationException();
    }

  }
  @Override
  public <T, E extends Element> ConvertsValue<T, PotentialNode<E>> toElementBuilder(final Class<? super T> cls, Class<?> template) {
    return new UnsupportedConverter<T, PotentialNode<E>>() {
      @SuppressWarnings({
          "unchecked", "rawtypes"
      } )
      @Override
      @GwtIncompatible
      public PotentialNode<E> convert(T from) {
        Class c = cls;// Erase generics; gwt doesn't like them much
        return new PotentialNode<E>(X_Html.toHtml(template, c, from, ElementalServiceDefault.this));
      }
    };
  }

  @Override
  public String enhanceMarkup(String markup) {
    if (lexer == null) {
      return markup;
    }
    try {
      return lexer.lex(markup).toString();
    } finally {
      lexer.clear();
    }
  }

  /**
   * @return the lexer
   */
  public Lexer getLexer() {
    return lexer;
  }

  /**
   * @param lexer the lexer to set
   */
  @Override
  public void setLexer(Lexer lexer) {
    this.lexer = lexer;
  }

  @Override
  public <E extends Element> PotentialNode<E> newNode(String tagname) {
    return new PotentialNode<E>(tagname);
  }

  @Override
  public <E extends Element> PotentialNode<E> newNode(E node) {
    return new PotentialNode<E>(node);
  }

  @Override
  public <E extends Element> PotentialNode<E> newNode() {
    return new PotentialNode<E>();
  }

  @Override
  public <E extends Element> E initialize(E element) {
    return element;
  }

  @Override
  public <E extends Element> ConvertsValue<E, E> asConverter() {
    return new ConvertsValue<E, E>() {
      @Override
      public E convert(E from) {
        return initialize(from);
      }
    };
  }

  @Override
  public native boolean hasShadowRoot(Element element)
  /*-{
    return element.shadowRoot !== undefined;
  }-*/;

  @Override
  public native Element getShadowRoot(Element element)
  /*-{
      if (element.shadowRoot) {
          return element.shadowRoot;
      }
      if (element.attachShadow) {
          return element.attachShadow({mode: "open"});
      }
      if (element.createShadowRoot) {
          return element.createShadowRoot();
      }
      return element;
  }-*/;

  @Override
  public native Element getShadowHost(Element element)
  /*-{
    return element.host || element;
  }-*/;

  @Override
  public String escapeHTML(String html) {
    escaper.setInnerText(html);
    return escaper.getInnerHTML();
  }

  @Override
  public String unescapeHTML(String html) {
    // extremely fast, and appears to be secure,
    // as both < and &lt; result in < character being output
    escaper.setInnerHTML(html);
    return escaper.getTextContent();
  }
  /** From angular JS: (use an equivalent of this for jvm elemental service

   SURROGATE_PAIR_REGEXP = /[\uD800-\uDBFF][\uDC00-\uDFFF]/g,
   // Match everything outside of normal chars and " (quote character)
   NON_ALPHANUMERIC_REGEXP = /([^\#-~| |!])/g;

   function decodeEntities(value) {
   if (!value) { return ''; }

   hiddenPre.innerHTML = value.replace(/</g,"&lt;");
   // innerText depends on styling as it doesn't display hidden elements.
   // Therefore, it's better to use textContent not to cause unnecessary reflows.
   return hiddenPre.textContent;
   }

  function encodeEntities(value) {
    return value.
        replace(/&/g, '&amp;').
    replace(SURROGATE_PAIR_REGEXP, function(value) {
      var hi = value.charCodeAt(0);
      var low = value.charCodeAt(1);
      return '&#' + (((hi - 0xD800) * 0x400) + (low - 0xDC00) + 0x10000) + ';';
    }).
    replace(NON_ALPHANUMERIC_REGEXP, function(value) {
      return '&#' + value.charCodeAt(0) + ';';
    }).
    replace(/</g, '&lt;').
    replace(/>/g, '&gt;');
  }


   * */
}
