package xapi.ui.api.event;

import xapi.event.api.IsEventType;
import xapi.fu.Log;
import xapi.log.X_Log;
import xapi.model.api.Model;
import xapi.ui.api.UiElement;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/17/16.
 */
public interface UiEventContext<Payload, Node, Base extends UiElement<Node, ? extends Node, Base>> extends IsEventType, Model, Log {

    String KEY_EVENT_TYPE = "eventType";
    String KEY_PAYLOAD_TYPE = "payload";
    String KEY_NATIVE_EVENT_TYPE = "nativeType";
    String KEY_NATIVE_EVENT_TARGET = "eventTarget";
    String KEY_SOURCE_ID = "sourceTarget";

    String DEFAULT_UI_TYPE = "ui";

    @Override
    default String getEventType() {
        return getProperty(KEY_EVENT_TYPE, DEFAULT_UI_TYPE);
    }

    default UiEventContext<Payload, Node, Base> setEventType(String type) {
        setProperty(KEY_EVENT_TYPE, type);
        return this;
    }

    default String getNativeType() {
        return getProperty(KEY_NATIVE_EVENT_TYPE, DEFAULT_UI_TYPE);
    }

    default UiEventContext<Payload, Node, Base> setNativeType(String type) {
        setProperty(KEY_NATIVE_EVENT_TYPE, type);
        return this;
    }

    /**
     *
     * @param node - The raw ui node that fired this event.
     * @return true to allow null,
     * false to add a warning and an assert:false : "Error message";
     * or, most preferably, THROW YOUR OWN EXCEPTIONS WITH MORE CONTEXT.
     */
    default boolean validateTarget(Node node) {
        return node != null;
    }

    default boolean validateSource(Base node) {
        return node != null;
    }

    default Node getNativeEventTarget() {
        return getProperty(KEY_NATIVE_EVENT_TARGET, ()->{
            // This is the default handler; if nothing was specified, check before we return null.
            if (validateTarget(null)) {
                return null;
            }
            log(LogLevel.WARN);
            X_Log.warn(getClass(), "Type", getClass(), "does not allow null target events.  Me: ", this);
            assert false : "Type " + getClass() + " does not allow null target events.  Me: " + this;
            return null;
        });
    }

    default UiEventContext<Payload, Node, Base> setNativeEventTarget(Node node) {
        setProperty(KEY_NATIVE_EVENT_TARGET, node);
        return this;
    }

    default Base getSourceElement() {
        return getProperty(KEY_SOURCE_ID, ()->{
            // This is the default handler; if nothing was specified, check before we return null.
            if (validateSource(null)) {
                return null;
            }
            log(LogLevel.WARN);
            X_Log.warn(getClass(), "Type", getClass(), "does not allow null sources events.  Me: ", this);
            assert false : "Type " + getClass() + " does not allow null target events.  Me: " + this;
            return null;
        });
    }

    default UiEventContext<Payload, Node, Base> setSource(Base ui) {
        setProperty(KEY_SOURCE_ID, ui);
        return this;
    }

    default Payload getPayload() {
        return getProperty(KEY_PAYLOAD_TYPE);
    }

    default UiEventContext<Payload, Node, Base> setPayload(Payload payload) {
        setProperty(KEY_PAYLOAD_TYPE, payload);
        return this;
    }

    @Override
    default void print(LogLevel level, String debug) {
        X_Log.log(getClass(), xapi.log.api.LogLevel.valueOf(level.name()), debug);
    }
}
