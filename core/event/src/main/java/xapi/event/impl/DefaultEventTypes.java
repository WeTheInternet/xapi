package xapi.event.impl;

import xapi.event.api.IsEventType;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 12/10/17.
 */
public enum DefaultEventTypes implements IsEventType {
    NAMED("named"),
    CHANGE("change"),
    DO("do"),
    DONE("done"),
    SELECT("select"),
    DESELECT("deselect"),
    ;

    private final String type;

    DefaultEventTypes(String type) {
        this.type = type;
    }

    @Override
    public String getEventType() {
        return type;
    }
}
