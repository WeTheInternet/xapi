package xapi.ui.api.event;

import xapi.event.impl.ChangeEvent;
import xapi.ui.api.UiElement;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/17/16.
 */
public interface UiChangeEvent<Payload, Node, Ui extends UiElement<Node, ? extends Node, Ui>, Type>
    extends ChangeEvent<UiEventContext<Payload, Node, Ui>, Type>,
    UiEvent<Payload, Node, Ui> {


}
