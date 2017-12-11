package xapi.event.api;

import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.collect.api.StringTo;
import xapi.event.impl.EventTypes;
import xapi.inject.X_Inject;
import xapi.log.X_Log;
import xapi.util.api.RemovalHandler;

import static xapi.collect.X_Collect.newStringMultiMap;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/16/16.
 */
public class EventManager implements Serializable {

    protected final StringTo.Many<EventHandler<?, ?>> handlers;
    protected final EventService eventService;

    public EventManager() {
        this(X_Inject.singleton(EventService.class));
    }

    public EventManager(EventService service) {
        this.eventService = service;
        handlers = newStringMultiMap(EventHandler.class, X_Collect.MUTABLE_INSERTION_ORDERED_SET);
    }

    public <Source, E extends IsEvent<Source>> RemovalHandler addHandler(IsEventType type, EventHandler<Source, E> lambda) {
        // method references, by default, do NOT conform to object equality semantics.
        // very bad things will happen if you add a non-serializable lambda and then try to use .removeHandler later.
        final EventHandler<Source, E> handler = eventService.normalizeHandler(lambda);
        final IntTo<EventHandler<?, ?>> handles = handlers.get(type.getEventType());
        boolean added = handles.add(handler);
        if (!added) {
            X_Log.warn(EventManager.class, "Added duplicate handler?", handler, handles.get(handles.indexOf(handler)));
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
            final EventHandler<Source, E> normalized = eventService.normalizeHandler(lambda);
            if (normalized != lambda) {
                removed = handles.removeValue(normalized);
            }
            if (!removed) {
                assert !eventService.isLambda(lambda) : "CANNOT REMOVE A LAMBDA HANDLER CORRECTLY! " + lambda + " is a raw lambda,\n" +
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

    public boolean handlesEvent(IsEventType type) {
        return handlers.containsKey(type.getEventType());
    }

    public boolean fireEvent(@NotNull IsEvent<?> event) {
        IntTo<EventHandler<?, ?>> handles = handlers.get(event.getTypeString());
        if (handles.isEmpty()) {
            handles = handlers.get(EventTypes.Unhandled.getEventType());
        }
        boolean allow = true;
        for (EventHandler handle : handles.forEach()) {
            allow = handle.handleEvent(event);
            if (!allow) {
                return false;
            }
        }
        return true;
    }

}
