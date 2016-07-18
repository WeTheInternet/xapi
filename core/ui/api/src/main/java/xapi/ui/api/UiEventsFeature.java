package xapi.ui.api;

import xapi.event.api.EventHandler;
import xapi.event.api.EventManager;
import xapi.event.api.IsEvent;
import xapi.event.api.IsEventType;
import xapi.ui.service.UiService;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/17/16.
 */
public class UiEventsFeature <Node, Base extends UiElement<Node, ? extends Node, Base>> implements UiFeature <Node, Base> {

    EventManager captures;
    EventManager bubbles;
    private UiService service;
    private Base node;

    @Override
    public void initialize(Base node, UiService service) {
        captures = service.newEventManager();
        bubbles = service.newEventManager();
        this.service = service;
        this.node = node;
    }

    public boolean handlesCapture(IsEventType type) {
        return captures.handlesEvent(type);
    }

    public boolean fireCapture(IsEvent<?> event) {
        return captures.fireEvent(event);
    }

    public boolean fireBubble(IsEvent<?> event) {
        return bubbles.fireEvent(event);
    }

    public boolean handlesBubble(IsEventType type) {
        return captures.handlesEvent(type);
    }

    public UiEventsFeature addCapturing(IsEventType type, EventHandler handler) {
        captures.addHandler(type, handler);
        return this;
    }

    public UiEventsFeature addBubbling(IsEventType type, EventHandler handler) {
        bubbles.addHandler(type, handler);
        service.bindEvent(type, node, handler, false);
        return this;
    }


}
