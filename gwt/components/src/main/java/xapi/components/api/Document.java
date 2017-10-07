package xapi.components.api;

import elemental.dom.Element;
import elemental.js.html.JsBodyElement;
import elemental.js.html.JsHTMLCollection;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import xapi.fu.iterate.ElementIterable;
import xapi.fu.MappedIterable;

import com.google.gwt.core.client.JavaScriptObject;

@JsType(isNative = true)
public interface Document {

  @JsType(isNative = true)
  interface RegisterElement {
    JavaScriptObject call(Document doc, String name, JavaScriptObject prototype);
  }

  @JsProperty
  Document.RegisterElement getRegisterElement();

  @SuppressWarnings("unusable-by-js")
  <E extends Element> E createElement(String string);

  @SuppressWarnings("unusable-by-js")
  <E extends Element> E getElementById(String id);

  JsHTMLCollection getElementsByTagName(String id);

  @JsOverlay
  default MappedIterable<Element> getByTagName(String id) {
    return ElementIterable.forEach(getElementsByTagName(id));
  }

  @JsProperty
  JsBodyElement getBody();

  /** filthy trick with this generic... */
  <N> N importNode(N content, boolean b);
}
