package xapi.components.api;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * Represents a class function.  Exposes its prototype to callers.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 7/15/17.
 */
@JsType(isNative = true)
public interface JsClass {

    @JsProperty
    JsPrototype getPrototype();

}
