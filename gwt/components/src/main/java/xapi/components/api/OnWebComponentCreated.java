package xapi.components.api;

import elemental.dom.Element;

public interface OnWebComponentCreated <E extends Element> extends WebComponentCallback {

  @NativelySupported
  void onCreated(E element);

}
