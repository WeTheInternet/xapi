package xapi.components.api;

import elemental.dom.Element;

public interface OnWebComponentAttached <E extends Element> extends WebComponentCallback {

  @NativelySupported
  void onAttached(E element);

}
