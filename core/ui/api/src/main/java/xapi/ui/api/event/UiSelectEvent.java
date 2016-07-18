package xapi.ui.api.event;

import xapi.event.impl.SelectEvent;
import xapi.ui.api.UiElement;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/17/16.
 */
public interface UiSelectEvent<Payload, Node, Ui extends UiElement<Node, ? extends Node, Ui>>
    extends UiEvent<Payload, Node, Ui>,
    SelectEvent<UiEventContext<Payload, Node, Ui>> {

}
