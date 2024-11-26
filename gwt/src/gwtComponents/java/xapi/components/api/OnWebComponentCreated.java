package xapi.components.api;

import elemental.dom.Element;
import jsinterop.annotations.JsType;

@JsType
public interface OnWebComponentCreated <E extends Element> extends WebComponentCallback {

  @NativelySupported
  void onCreated(E element);

}
