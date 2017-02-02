package xapi.components.api;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/27/17.
 */
@JsType(isNative = true)
public interface JsObjectDescriptor {

    @JsProperty
    boolean getConfigurable();
    @JsProperty
    void setConfigurable(boolean configurable);

    @JsProperty
    boolean getEnumerable();
    @JsProperty
    void setEnumerable(boolean enumerable);

    @JsProperty
    boolean getWritable();
    @JsProperty
    void setWritable(boolean writable);

    @JsProperty
    Object getValue();
    @JsProperty
    void setValue(Object value);

    @JsProperty(name = "get")
    JavaScriptObject get();
    @JsProperty(name = "get")
    void get(JavaScriptObject getter);

    @JsProperty(name = "set")
    JavaScriptObject set();
    @JsProperty(name = "set")
    void set(JavaScriptObject setter);
}
