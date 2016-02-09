/**
 *
 */
package xapi.elemental.api;

import elemental.dom.Element;
import elemental.html.StyleElement;
import xapi.source.api.Lexer;
import xapi.ui.api.StyleService;
import xapi.util.api.ConvertsValue;

import com.google.gwt.core.client.MagicMethod;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

/**
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public interface ElementalService extends StyleService<ElementalService> {

  String METHOD_ENHANCE_MARKUP = "enhanceMarkup";
  String METHOD_TO_ELEMENT = "toElement";
  String METHOD_TO_ELEMENT_BUILDER = "toElementBuilder";

  <E extends Element> ConvertsValue<E, E> asConverter();

  String enhanceMarkup(String markup);

  <E extends Element> E initialize(E element);

  <E extends Element> PotentialNode<E> newNode();

  <E extends Element> PotentialNode<E> newNode(E node);

  <E extends Element> PotentialNode<E> newNode(String tagName);

  void setLexer(Lexer lexer);

  @MagicMethod(doNotVisit=true)
  <T, E extends Element> E toElement(Class<? super T> cls, Class<?> template, T obj);

  @MagicMethod(doNotVisit=true)
  <T, E extends Element> E toElement(Class<? super T> cls, T obj);

  @MagicMethod(doNotVisit=true)
  <T, E extends Element> ConvertsValue<T, PotentialNode<E>> toElementBuilder(Class<? super T> cls);

  @MagicMethod(doNotVisit=true)
  <T, E extends Element> ConvertsValue<T, PotentialNode<E>> toElementBuilder(Class<? super T> cls, Class<?> template);

  Element getShadowRoot(Element element);

  String escapeHTML(String html);

  String unescapeHTML(String html);

  StyleElement injectStyle(Class<? extends ClientBundle> bundle, Class<? extends CssResource> ... styles);

  ElementalService registerStyle(Class<? extends ClientBundle> bundle, String css, Class<? extends CssResource> ... styles);

}
