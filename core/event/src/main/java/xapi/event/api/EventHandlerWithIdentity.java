package xapi.event.api;

import xapi.util.api.RemovalHandler;

import static xapi.util.api.SerializableWrapper.serializableId;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Objects;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/16/16.
 */
public class EventHandlerWithIdentity<Source, Event extends IsEvent<Source>> implements EventHandler<Source, Event> {

    private final Serializable id;
    public final EventHandler<Source, Event> wrapped;

    public EventHandlerWithIdentity(@NotNull Serializable id, @NotNull EventHandler<Source, Event> wrapped) {
        this.id = serializableId(id);
        this.wrapped = wrapped;
    }

    public void handleEvent(Event e) {
        wrapped.handleEvent(e);
    }

    @Override
    public void storeRemover(RemovalHandler remover) {
        wrapped.storeRemover(remover);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof EventHandlerWithIdentity))
            return false;

        final EventHandlerWithIdentity<?, ?> that = (EventHandlerWithIdentity<?, ?>) o;

        return Objects.equals(id, that.id);

    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

}
