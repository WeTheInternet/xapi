package xapi.components.api;

import elemental.dom.Element;
import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsType;
import xapi.components.impl.WebComponentSupport;

@JsType
public interface IsWebComponent<E extends Element> extends HasElement<E> {

  @Override
  @JsIgnore
  default E element() {
    return WebComponentSupport.asElement(this);
  }

}
