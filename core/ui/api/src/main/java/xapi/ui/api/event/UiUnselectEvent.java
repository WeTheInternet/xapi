package xapi.ui.api.event;

import xapi.event.impl.UnselectEvent;
import xapi.ui.api.UiElement;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/17/16.
 */
public interface UiUnselectEvent<Payload, Node, Ui extends UiElement<Node, ? extends Node, Ui>>
    extends UiEvent<Payload, Node, Ui>,
    UnselectEvent<UiEventContext<Payload, Node, Ui>> {

}
