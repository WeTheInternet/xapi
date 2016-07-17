package xapi.event.api;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/16/16.
 */
public interface IsEvent <Source> {

    Source getSource();

    IsEventType getType();

    default String getTypeString() {
        return getType().getEventType();
    }
}
