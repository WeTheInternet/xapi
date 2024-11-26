package xapi.components.api;

import jsinterop.annotations.JsType;

@JsType(isNative = true)
public interface Console {
	void log(Object message);
}
