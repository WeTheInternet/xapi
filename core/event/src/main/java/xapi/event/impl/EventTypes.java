package xapi.event.impl;

import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.event.api.IsEventType;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/16/16.
 */
public enum EventTypes implements IsEventType {
    Named("named"),
    Unlock("unlock"),

    Select("select"),
    Unselect("unselect"),

    Change("change"),
    Undo("undo"),

    Unhandled("unhandled"),

    Click("click"),
    LongClick("longclick"),
    DoubleClick("doubleclick"),

    Move("move"),
    Scroll("scroll"),

    Hover("hover"),
    Unhover("unhover"),

    Focus("focus"),
    Blur("blur"),

    DragStart("dragstart"),
    DragEnd("dragend"),
    DragMove("dragmove"),

    Resize("resize"),
    Attach("attach"),
    Detach("detach");

    private static final StringTo<IsEventType> types = X_Collect.newStringMap(IsEventType.class);
    static {
        for (EventTypes type : values()) {
            types.put(type.eventType, type);
        }

    }
    private final String eventType;

    EventTypes(String eventType) {
        this.eventType = eventType;
    }

    @Override
    public String getEventType() {
        return eventType;
    }

    public static IsEventType convertType(String nativeType) {
        return types.get(nativeType);
    }

    public static IsEventType registerType(String nativeType, IsEventType type) {
        return types.put(nativeType, type);
    }
}
