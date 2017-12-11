package xapi.event.impl;

import xapi.event.api.IsEvent;
import xapi.event.api.IsEventType;
import xapi.fu.has.HasName;
import xapi.util.X_Debug;
import xapi.util.api.HasId;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 12/10/17.
 */
public interface NamedEvent <Source> extends IsEvent<Source>, HasName {

    @Override
    default String getName() {
        return getNameFrom(getSource());
    }

    @Override
    default IsEventType getType() {
        return EventTypes.Named;
    }

    default String getNameFrom(Source source){
        if (source instanceof HasName) {
            return ((HasName) source).getName();
        }
        if (source instanceof CharSequence) {
            return source.toString();
        }
        if (source == null) {
            return "";
        }
        if (source instanceof HasId) {
            return ((HasId) source).getId();
        }
        assert false : "Cannot extract name from " + source.getClass()+" : " + source;
        throw X_Debug.recommendAssertions();
    }

}
