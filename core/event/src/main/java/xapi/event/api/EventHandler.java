package xapi.event.api;

import xapi.fu.Filter.Filter1;
import xapi.util.api.RemovalHandler;

import java.io.Serializable;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/16/16.
 */
public interface EventHandler <Source, Event extends IsEvent<Source>> extends Serializable {

    boolean handleEvent(Event e);

    /**
     * This method is provided, for your convenience, to store a reference to the event handler remover object.
     *
     * In cases where you want to use non-serializable lambdas as EventHandlers, this gives you the opportunity
     * to stash the removal handler in a way that allows you to remove the handler from an EventManager.
     *
     * Note that lambdas and method references, by default, do not obey sane object equality semantics,
     * so we internally use a trick to serialize them to byte[] and implement hashCode and equals with said byte[].
     *
     * Since this is fairly heavyweight, you may instead opt to use a stashed RemovalHandler to disconnect event handlers.
     */
    default void storeRemover(RemovalHandler remover) {

    }

    /**
     * A convenience adapter for creating removable event handlers.  Since standard lambdas and method references
     * do not object object-identity, we do a bit of bending over backwards to provide simple-to-use methods for
     * creating event handlers that actually can be removed.
     *
     * If your lambda closes entirely over serializable instances / references, then a simple lambda will be transformed
     * into a removable one by our {@link xapi.event.impl.EventServiceDefault}, which is done by the somewhat-nasty
     * method of serializing the lambda into bytes, so we can implement equals and hashcode correctly.
     *
     * If you don't want to (or can't) pay to serialize potentially heavyweight object just to make them removable,
     * then you need to utilize another (lighter weight) means of providing object-identity for our
     * {@link EventManager} to be able to remove your lambda.
     *
     * If you actually hold onto and use the {@link RemovalHandler} returned from {@link EventManager#addHandler(IsEventType, EventHandler)},
     * or if you hold onto the ad-hoc created anonymous lambda instance to call {@link EventManager#removeHandler(IsEventType, EventHandler)},
     * then you DON'T need any of these extra methods (however, by default, we do wrap all serializable lambdas).
     *
     * The returned RemovalHandler closes over the actual lambda instance you provide, and is the only bit of code other
     * than the original generated lambda capable of finding and removing the handler from the manager's internal map.
     *
     * This handy static method allows you to provide any serializable key, and an {@link Filter1} instance, which calls
     * into the constructor of our {@link RemovableEventHandler} class (which you can feel free to instantiate directly)
     *
     */
    static <Source, Event extends IsEvent<Source>> RemovableEventHandler<Source, Event> removable(Serializable id, Filter1<Event> handler) {
        return new RemovableEventHandler<>(id, handler::filter1);
    }
}
