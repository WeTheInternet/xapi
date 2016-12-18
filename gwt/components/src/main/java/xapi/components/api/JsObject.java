package xapi.components.api;

import jsinterop.annotations.JsType;

import com.google.gwt.core.client.JavaScriptObject;

@JsType
public interface JsObject {

	<T> T create(T clone);

	void defineProperty(Object onto, String name, JavaScriptObject properties);
}
