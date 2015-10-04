/**
 *
 */
package xapi.elemental.impl;

import com.google.gwt.core.shared.GwtIncompatible;

import elemental.dom.Element;

import xapi.annotation.inject.SingletonDefault;
import xapi.elemental.api.ElementalService;
import xapi.elemental.api.PotentialNode;
import xapi.source.api.Lexer;
import xapi.source.impl.LexerForWords;
import xapi.source.impl.StringCharIterator;
import xapi.ui.html.X_Html;
import xapi.ui.html.impl.StyleServiceDefault;
import xapi.util.api.ConvertsValue;

/**
 * @author "James X. Nelson (james@wetheinter.net)"
 */
@SingletonDefault(implFor=ElementalService.class)
public class ElementalServiceDefault
extends StyleServiceDefault<ElementalService>
implements ElementalService {

  private Lexer lexer;

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

}
