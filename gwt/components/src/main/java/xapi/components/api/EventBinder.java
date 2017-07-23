package xapi.components.api;

import elemental.events.Event;
import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.fu.In1;
import xapi.fu.iterate.Chain;
import xapi.fu.iterate.ChainBuilder;
import xapi.util.api.RemovalHandler;

/**
 * In GWT, event listeners bound directly to an element will bypass
 * the $entry() wrapping (which fires scheduled / finally commands),
 * and even if we manually wrap every handler, they will each fire
 * individually, each cleaning out scheduleFinally commands.
 *
 * To bypass this, for events we want to bind to, we will create
 * the native binding here, and just maintain our own list of callbacks.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 6/4/17.
 */
public class EventBinder {

    private final Object target;
    private final IntTo<In1<Event>> handlers;

    public EventBinder(Object target, String evtName, boolean capturePhase) {
        this.target = target;
        addEventListener(target, evtName, capturePhase);
        handlers = X_Collect.newList(In1.class);
    }

    public RemovalHandler addEventListener(In1<Event> listener) {
        assert !handlers.contains(listener) : "Do not add duplicate listeners: " + listener;
        handlers.add(listener);
        return ()->handlers.removeValue(listener);
    }

    private native void addEventListener(Object target, String evtName, boolean capturePhase)
    /*-{
        var self = this;
        target.addEventListener(evtName, function(e){
          self.@xapi.components.api.EventBinder::onEvent(Lelemental/events/Event;)(e);
        }, capturePhase);
    }-*/;

    @SuppressWarnings("unused") // called by jsni, atm.
    private void onEvent(Event e) {
        handlers
            .forEachItem()
            .forAll(In1::in, e);
    }
}
