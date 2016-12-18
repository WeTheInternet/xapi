package xapi.components.api;

import elemental.events.Event;
import jsinterop.annotations.JsFunction;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/28/16.
 */
@JsFunction
public interface JsEventListener <T extends Event> {
    void onEvent(T event);
}
