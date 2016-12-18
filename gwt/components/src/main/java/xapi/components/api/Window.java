package xapi.components.api;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(isNative = true)
public interface Window {

  @JsProperty
  Document getDocument();

  @JsProperty
  int getInnerWidth();

  @JsProperty
  int getInnerHeight();

  void addEventListener(String name, JsEventListener<?> callback, boolean useCapture);

  void removeEventListener(String name, JsEventListener<?> callback, boolean useCapture);

}
