package xapi.ui.api.event;

import xapi.event.api.IsEvent;
import xapi.ui.api.UiElement;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/16/16.
 */
public interface UiEvent <Payload, Node, Ui extends UiElement<Node, ? extends Node, Ui>> extends IsEvent<UiEventContext<Payload, Node, Ui>> {

}
