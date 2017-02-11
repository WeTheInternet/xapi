package xapi.components.api;

import elemental.dom.Element;
import elemental.js.html.JsBodyElement;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

import com.google.gwt.core.client.JavaScriptObject;

@JsType(isNative = true)
public interface Document {

  @JsType(isNative = true)
  interface RegisterElement {
    JavaScriptObject call(Document doc, String name, JavaScriptObject prototype);
  }

  @JsProperty
  Document.RegisterElement getRegisterElement();

  <E extends Element> E createElement(String string);

  <E extends Element> E getElementById(String id);

  @JsProperty
  JsBodyElement getBody();

  /** filthy trick with this generic... */
  <N> N importNode(N content, boolean b);
}
