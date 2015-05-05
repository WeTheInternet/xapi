package xapi.components.api;

import com.google.gwt.core.client.js.JsType;

import xapi.components.impl.WebComponentSupport;
import elemental.dom.Element;

@JsType
public interface IsWebComponent<E extends Element> extends HasElement<E> {

  @Override
  default E element() {
    return WebComponentSupport.asElement(this);
  }

}
