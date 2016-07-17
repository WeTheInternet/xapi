package xapi.event.api;

import java.io.Serializable;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/16/16.
 */
public interface EventService extends Serializable {

    <Source, Event extends IsEvent<Source>> EventHandler<Source, Event> normalizeHandler(EventHandler<Source, Event> handler);

    boolean isLambda(EventHandler<?, ?> lambda);
}
