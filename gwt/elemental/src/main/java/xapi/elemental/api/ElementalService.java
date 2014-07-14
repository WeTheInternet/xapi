/**
 *
 */
package xapi.elemental.api;

import elemental.dom.Element;
import xapi.source.api.Lexer;
import xapi.ui.api.StyleService;
import xapi.util.api.ConvertsValue;
import elemental.dom.Element;


/**
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public interface ElementalService extends StyleService<ElementalService> {

  String METHOD_ENHANCE_MARKUP = "enhanceMarkup";
  String METHOD_TO_ELEMENT = "toElement";
  String METHOD_TO_ELEMENT_BUILDER = "toElementBuilder";

  <T, E extends Element> E toElement(Class<? super T> cls, T obj);

  <T, E extends Element> E toElement(Class<? super T> cls, Class<?> template, T obj);

  <T, E extends Element> ConvertsValue<T, PotentialNode<E>> toElementBuilder(Class<? super T> cls);

  <T, E extends Element> ConvertsValue<T, PotentialNode<E>> toElementBuilder(Class<? super T> cls, Class<?> template);

  void loadGoogleFonts(String ... fonts);

  String enhanceMarkup(String markup);

  void setLexer(Lexer lexer);

  <E extends Element> PotentialNode<E> newNode(String tagName);

  <E extends Element> PotentialNode<E> newNode(E node);

  <E extends Element> PotentialNode<E> newNode();

  <E extends Element> E initialize(E element);

  <E extends Element> ConvertsValue<E, E> asConverter();

}
