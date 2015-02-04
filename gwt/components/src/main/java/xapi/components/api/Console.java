package xapi.components.api;

import com.google.gwt.core.client.js.JsType;

@JsType(prototype="Console")
public interface Console {
	public void log(Object message);
}