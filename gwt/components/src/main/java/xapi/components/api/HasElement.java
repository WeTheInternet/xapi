package xapi.components.api;

import com.google.gwt.core.client.js.JsType;

import elemental.dom.Element;

@JsType
public interface HasElement<E extends Element> {

  // We don't use .getElement() to avoid colliding w/ existing GWT apis
  @NativelySupported
  E element();

}
