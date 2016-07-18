package xapi.event.api;

import xapi.collect.api.IntTo;
import xapi.util.api.RemovalHandler;

import static xapi.collect.X_Collect.newSet;
import static xapi.util.api.SerializableWrapper.serializableId;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/16/16.
 */
public class RemovableEventHandler <Source, Event extends IsEvent<Source>> implements EventHandler<Source, Event>,
    Serializable, RemovalHandler {

    private final EventHandler<Source, Event> lambda;
    private final IntTo<RemovalHandler> removers;
    private final Serializable id;

    public RemovableEventHandler(@NotNull EventHandler<Source, Event> lambda) {
        this(lambda, lambda);
    }

    public RemovableEventHandler(@NotNull Serializable id, @NotNull EventHandler<Source, Event> lambda) {
        this.lambda = lambda;
        this.id = serializableId(id);
        removers = newSet(RemovalHandler.class);
    }

    @Override
    public boolean handleEvent(Event e) {
        return lambda.handleEvent(e);
    }

    @Override
    public void storeRemover(RemovalHandler remover) {
        removers.add(remover);
    }

    public void remove() {
        removers.removeAll(RemovalHandler::remove);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof RemovableEventHandler))
            return false;

        final RemovableEventHandler<?, ?> that = (RemovableEventHandler<?, ?>) o;

        return id.equals(that.id);

    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
