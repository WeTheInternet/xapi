package xapi.components.api;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.js.JsProperty;
import com.google.gwt.core.client.js.JsType;

import elemental.dom.Element;
import elemental.html.BodyElement;

@JsType
public interface Document {
  @JsType
  public static interface RegisterElement {
    JavaScriptObject call(Document doc, String name, JavaScriptObject prototype);
  }

  @JsProperty
  Document.RegisterElement getRegisterElement();

  <E extends Element> E createElement(String string);

  <E extends Element> E getElementById(String id);

  @JsProperty
  BodyElement getBody();
}