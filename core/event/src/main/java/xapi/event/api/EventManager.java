package xapi.event.api;

import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.collect.api.StringTo;
import xapi.event.impl.EventTypes;
import xapi.fu.In1;
import xapi.inject.X_Inject;
import xapi.log.X_Log;
import xapi.util.api.RemovalHandler;

import static xapi.collect.X_Collect.newStringMultiMap;
import static xapi.fu.In2.reduce2;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/16/16.
 */
public class EventManager implements Serializable {

    private final StringTo.Many<EventHandler<?, ?>> handlers;
    private final EventService service;

    public EventManager() {
        this(X_Inject.singleton(EventService.class));
    }

    public EventManager(EventService service) {
        this.service = service;
        handlers = newStringMultiMap(EventHandler.class, X_Collect.MUTABLE_INSERTION_ORDERED_SET);
    }

    public <Source, E extends IsEvent<Source>> RemovalHandler addHandler(IsEventType type, EventHandler<Source, E> lambda) {
        // method references, by default, do NOT conform to object equality semantics.
        // very bad things will happen if you add a non-serializable lambda and then try to use .removeHandler later.
        final EventHandler<Source, E> handler = service.normalizeHandler(lambda);
        final IntTo<EventHandler<?, ?>> handles = handlers.get(type.getEventType());
        boolean added = handles.add(handler);
        if (!added) {
            X_Log.warn(getClass(), "Added duplicate handler?", handler, handles.get(handles.indexOf(handler)));
        }
        final RemovalHandler remover = () -> handles.remove(handler);
        lambda.storeRemover(remover);
        return remover;
    }

    public <Source, E extends IsEvent<Source>> boolean removeHandler(IsEventType type, EventHandler<Source, E> lambda) {
        final IntTo<EventHandler<?, ?>> handles = handlers.get(type.getEventType());
        if (handles.isEmpty()) {
            return false;
        }
        boolean removed = handles.removeValue(lambda);
        if (!removed) {
            final EventHandler<Source, E> normalized = service.normalizeHandler(lambda);
            if (normalized != lambda) {
                removed = handles.removeValue(normalized);
            }
            if (!removed) {
                assert !service.isLambda(lambda) : "CANNOT REMOVE A LAMBDA HANDLER CORRECTLY! " + lambda + " is a raw lambda,\n" +
                    "which are created as ad-hoc anonymous classes that do NOT conform to object identity semantics;\nif you want " +
                    "to be able to use EventManager.removeHandler correctly,\nyou MUST make sure any instance or closed-over references " +
                    "are Serializable AND immutable\n(and your EventService is configured to transform serializable lambdas into instances of " +
                    "EventHandlerWithIdentity),\nOR you must create your own EventHandler *class* which implements equals and " +
                    "hashCode correctly.\nIf you ignore this assertion, your application WILL leak memory / be unable to deregister " +
                    "event handlers (unless you store the RemovalHandler created in addHandler).\nYou have been warned!";
            }

        }
        return removed;
    }

    public <Source> void fireEvent(@NotNull IsEvent<Source> event) {
        IntTo<EventHandler<?, ?>> handles = handlers.get(event.getTypeString());
        if (handles.isEmpty()) {
            handles = handlers.get(EventTypes.Unhandled.getEventType());
        }
        final In1<EventHandler> r = reduce2(EventHandler::handleEvent, event);
        handles.forEachValue(handler->r.in(handler));
    }

}
