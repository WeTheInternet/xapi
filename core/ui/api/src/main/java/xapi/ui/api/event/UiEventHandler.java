package xapi.ui.api.event;

import xapi.event.api.EventHandler;
import xapi.ui.api.UiElement;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/16/16.
 */
public interface UiEventHandler
    <
        Payload,
        Node,
        Ui extends UiElement<Node, ? extends Node, Ui>,
        Event extends UiEvent<Payload, Node, Ui>
    >
    extends EventHandler <UiEventContext<Payload, Node, Ui>, Event> {

}
