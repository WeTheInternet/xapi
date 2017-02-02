package xapi.components.api;

import jsinterop.annotations.JsType;

@JsType(isNative = true, name = "Object")
public interface JsObject {

	<T> T create(T clone);

	void defineProperty(Object onto, Object name, JsObjectDescriptor properties);

}
