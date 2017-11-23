/**
 *
 */
package xapi.elemental.api;

import elemental2.dom.HTMLElement;
import xapi.fu.In1;
import xapi.source.api.Lexer;
import xapi.ui.api.ElementBuilder;
import xapi.ui.service.ElementService;

/**
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public interface BrowserService extends ElementService<HTMLElement> {

  String enhanceMarkup(String markup);

  <E extends HTMLElement> E initialize(E element);
  <E extends HTMLElement> ElementBuilder<E> newNode();
  <E extends HTMLElement> ElementBuilder<E> newNode(E node);
  <E extends HTMLElement> ElementBuilder<E> newNode(String tagName);
  void ensureAttached(HTMLElement element, In1<HTMLElement> whileAttached);

  void setLexer(Lexer lexer);

  HTMLElement getShadowRoot(HTMLElement element);
  boolean hasShadowRoot(HTMLElement element);
  HTMLElement getShadowHost(HTMLElement element);

  String escapeHTML(String html);
  String unescapeHTML(String html);

}
