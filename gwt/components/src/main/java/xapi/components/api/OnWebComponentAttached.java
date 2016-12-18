package xapi.components.api;

import elemental.dom.Element;
import jsinterop.annotations.JsType;

@JsType
public interface OnWebComponentAttached <E extends Element> extends WebComponentCallback {

  @NativelySupported
  void onAttached(E element);

}
