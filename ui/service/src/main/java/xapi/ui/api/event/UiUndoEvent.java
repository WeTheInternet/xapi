package xapi.ui.api.event;

import xapi.event.impl.UndoEvent;
import xapi.ui.api.UiElement;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/17/16.
 */
public interface UiUndoEvent<Payload, Node, Ui extends UiElement<Node, ? extends Node, Ui>, Type>
    extends UndoEvent<UiEventContext<Payload, Node, Ui>, Type>,
    UiEvent<Payload, Node, Ui> {

}
