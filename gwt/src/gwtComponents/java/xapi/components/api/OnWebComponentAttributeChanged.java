package xapi.components.api;

import jsinterop.annotations.JsType;

@JsType
public interface OnWebComponentAttributeChanged <E> extends WebComponentCallback {

  @NativelySupported
  void onAttributeChanged(E element, String name, String oldVal, String newVal);

}
