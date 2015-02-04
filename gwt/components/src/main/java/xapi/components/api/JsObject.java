package xapi.components.api;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.js.JsType;

@JsType
public interface JsObject {

	<T> T create(T clone);

	void defineProperty(Object onto, String name, JavaScriptObject properties);
}