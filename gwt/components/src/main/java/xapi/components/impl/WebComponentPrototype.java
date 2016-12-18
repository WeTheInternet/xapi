package xapi.components.impl;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/24/16.
 */
@JsType(isNative = true)
interface WebComponentPrototype {

  @JsProperty
  void setAttachedCallback(JavaScriptObject callback);

  @JsProperty
  JavaScriptObject getAttachedCallback();

  @JsProperty
  void setCreatedCallback(JavaScriptObject callback);

  @JsProperty
  JavaScriptObject getCreatedCallback();

  @JsProperty
  void setDetachedCallback(JavaScriptObject callback);

  @JsProperty
  JavaScriptObject getDetachedCallback();

  @JsProperty
  void setAttributeChangedCallback(JavaScriptObject callback);

  @JsProperty
  JavaScriptObject getAttributeChangedCallback();
}
