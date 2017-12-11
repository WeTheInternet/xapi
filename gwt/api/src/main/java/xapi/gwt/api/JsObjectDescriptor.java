package xapi.gwt.api;

import elemental2.core.Function;
import elemental2.core.ObjectPropertyDescriptor.GetCallbackFn;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import jsinterop.base.Js;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/27/17.
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL)
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
    GetCallbackFn get();
    @JsProperty(name = "get")
    void get(GetCallbackFn getter);

    @JsProperty(name = "set")
    Function set();
    @JsProperty(name = "set")
    void set(Function setter);

    @JsOverlay
    static JsObjectDescriptor create() {
        return Js.uncheckedCast(new Object());
    }
    @JsOverlay
    static JsObjectDescriptor createUnconfigurable() {
        final JsObjectDescriptor descriptor = create();
        descriptor.setConfigurable(false);
        descriptor.setEnumerable(false);
        return descriptor;
    }
    @JsOverlay
    static JsObjectDescriptor createImmutable() {
        final JsObjectDescriptor descriptor = create();
        descriptor.setConfigurable(false);
        descriptor.setWritable(false);
        descriptor.setEnumerable(false);
        return descriptor;
    }
}
