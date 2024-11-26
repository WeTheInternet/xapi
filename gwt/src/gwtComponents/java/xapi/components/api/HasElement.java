package xapi.components.api;

import elemental.dom.Element;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;

@JsType
public interface HasElement<E extends Element> {

  // We don't use .getElement() to avoid colliding w/ existing GWT apis
  @JsOverlay
  E element();

}
