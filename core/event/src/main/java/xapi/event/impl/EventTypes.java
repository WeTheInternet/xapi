package xapi.event.impl;

import xapi.event.api.IsEventType;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/16/16.
 */
public enum EventTypes implements IsEventType {
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

    private final String eventType;

    EventTypes(String eventType) {
        this.eventType = eventType;
    }

    @Override
    public String getEventType() {
        return eventType;
    }
}
