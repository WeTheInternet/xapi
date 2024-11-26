package xapi.components.api;

import elemental.dom.Element;
import jsinterop.annotations.JsType;

@JsType
public interface OnWebComponentDetached <E extends Element> extends WebComponentCallback {

  @NativelySupported
  void onDetached(E element);

}
