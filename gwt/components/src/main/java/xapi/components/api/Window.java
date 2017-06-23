package xapi.components.api;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import xapi.components.impl.JsFunctionSupport;

@JsType(isNative = true)
public interface Window {

  @JsProperty
  Document getDocument();

  @JsProperty
  int getInnerWidth();

  @JsProperty
  int getInnerHeight();

  @JsOverlay
  default void addEnteredListener(String name, JsEventListener<?> callback, boolean useCapture) {
    addEventListener(name, JsFunctionSupport.fixListener(callback), useCapture);
  }
  void addEventListener(String name, JsEventListener<?> callback, boolean useCapture);

  void removeEventListener(String name, JsEventListener<?> callback, boolean useCapture);

  void alert(String s);
}
