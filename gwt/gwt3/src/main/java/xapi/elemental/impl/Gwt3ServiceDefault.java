/**
 *
 */
package xapi.elemental.impl;

import elemental2.core.Reflect;
import elemental2.dom.DomGlobal;
import elemental2.dom.Element;
import elemental2.dom.HTMLElement;
import elemental2.dom.HTMLStyleElement;
import elemental2.dom.HTMLTextAreaElement;
import jsinterop.base.Js;
import xapi.annotation.inject.SingletonDefault;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.elemental.X_Gwt3;
import xapi.elemental.api.BrowserService;
import xapi.elemental.api.ElementalBuilder;
import xapi.fu.In1;
import xapi.fu.Lazy;
import xapi.source.api.Lexer;
import xapi.ui.style.StyleServiceAbstract;

/**
 * @author "James X. Nelson (james@wetheinter.net)"
 */
@SingletonDefault(implFor=BrowserService.class)
public class Gwt3ServiceDefault
extends StyleServiceAbstract<HTMLStyleElement>
implements BrowserService {

  private Lexer lexer;
  private final Lazy<Element> offscreen;
  private static final HTMLTextAreaElement escaper = (HTMLTextAreaElement) DomGlobal.document.createElement("textarea");

  public Gwt3ServiceDefault() {
    offscreen = Lazy.deferred1(()->{
      HTMLElement el = X_Gwt3.newDiv();
      el.style.position = "absolute";
      el.style.left = "-2000px";
      el.style.top = "-2000px";
      DomGlobal.document.body.appendChild(el);
      return el;
    });
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
  public <E extends HTMLElement> ElementalBuilder<E> newNode(String tagname) {
    return new ElementalBuilder<E>(tagname);
  }

  @Override
  public <E extends HTMLElement> ElementalBuilder<E> newNode(E node) {
    return new ElementalBuilder<E>(node);
  }

  @Override
  public <E extends HTMLElement> ElementalBuilder<E> newNode() {
    return new ElementalBuilder<>();
  }

  @Override
  public <E extends HTMLElement> E initialize(E element) {
    return element;
  }

  @Override
  public boolean hasShadowRoot(HTMLElement element) {
    return element.shadowRoot != null || Reflect.has(
        Js.uncheckedCast(element), "shadowController");
  }

  @Override
  public native HTMLElement getShadowRoot(HTMLElement element)
  /*-{
      if (element.shadowController) {
          return element.shadowController;
      }
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
  public native HTMLElement getShadowHost(HTMLElement element)
  /*-{
    return element.host || element;
  }-*/;

  @Override
  public String escapeHTML(String html) {
    escaper.textContent = html;
    return escaper.innerHTML;
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

  @Override
  public void ensureAttached(HTMLElement element, In1<HTMLElement> whileAttached) {
    if (element.parentNode != null) {
      whileAttached.in(element);
      return;
    }
    // TODO: consider an off-screen container that is actually rendered...
    offscreen.out1().appendChild(element);
    whileAttached.in(element);
    element.parentNode.removeChild(element);
  }
  @Override
  public String unescapeHTML(String html) {
    // extremely fast, and appears to be secure,
    // as both < and &lt; result in < character being output
    escaper.innerHTML = html;
    return escaper.textContent;
  }

  @Override
  protected StringTo<HTMLStyleElement> liveCssMap() {
    return X_Collect.newStringMap(HTMLStyleElement.class);
  }

  @Override
  protected HTMLStyleElement prioritizedStyle(String priority) {
    return null;
  }
}
