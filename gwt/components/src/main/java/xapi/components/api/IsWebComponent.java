package xapi.components.api;

import xapi.components.impl.WebComponentSupport;

import elemental.dom.Element;

public interface IsWebComponent<E extends Element> extends HasElement<E> {

  @Override
  default E element() {
    return WebComponentSupport.asElement(this);
  }

}
