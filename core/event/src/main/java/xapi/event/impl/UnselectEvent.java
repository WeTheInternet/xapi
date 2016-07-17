package xapi.event.impl;

import xapi.event.api.IsEvent;
import xapi.event.api.IsEventType;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/16/16.
 */
public interface UnselectEvent<Source> extends IsEvent<Source> {

    @Override
    default IsEventType getType() {
        return EventTypes.Unselect;
    }

    static <S> UnselectEvent<S> unselect(S source) {
        return () -> source;
    }
}
