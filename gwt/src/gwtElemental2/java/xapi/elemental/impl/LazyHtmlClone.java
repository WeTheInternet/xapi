package xapi.elemental.impl;

import javax.inject.Provider;

import elemental2.dom.Element;
import xapi.elemental.X_Gwt3;
import xapi.fu.Lazy;
import xapi.fu.Out1;
import xapi.util.api.ConvertsValue;

import static xapi.elemental.X_Gwt3.toElement;

public class LazyHtmlClone <E extends Element> implements Provider<E> {

  private final Out1<E> provider;
  @SuppressWarnings("unchecked" )
  private ConvertsValue<E, E> converter = ConvertsValue.PASS_THRU;

  public LazyHtmlClone(String html) {
    provider = Lazy.deferred1(X_Gwt3::toElement, html);
  }

  public LazyHtmlClone(Provider<String> html) {
    provider = Lazy.deferred1(() -> {
      String val = html.get();
      return val == null ? null : toElement(val);
    });
  }

  protected E init(E element) {
    return converter.convert(element);
  }

  public LazyHtmlClone<E> setInitializer(ConvertsValue<E, E> initializer) {
    converter = initializer;
    return this;
  }

  @SuppressWarnings({
      "rawtypes", "unchecked"
  } )
  public LazyHtmlClone(Element e) {
    provider = Out1.immutable((E)e);
  }

  @SuppressWarnings({
    "rawtypes", "unchecked"
  } )
  public LazyHtmlClone(Element e, String backup) {
    provider = Lazy.withBackup((E)e, () -> X_Gwt3.toElement(backup));
  }

  @Override
  @SuppressWarnings("unchecked")
  public E get() {
    return init((E)provider.out1().cloneNode(true));
  }
}
