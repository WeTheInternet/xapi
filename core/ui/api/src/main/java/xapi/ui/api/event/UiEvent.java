package xapi.ui.api.event;

import xapi.event.api.IsEvent;
import xapi.ui.api.UiElement;
import xapi.ui.api.UiNode;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/16/16.
 */
public interface UiEvent <Payload, Node, Ui extends UiNode<Node, ? extends Node, Ui>> extends IsEvent<UiEventContext<Payload, Node, Ui>> {

    default Ui ui() {
        return getSource().getSourceElement();
    }

    default Node node() {
        return getSource().getNativeEventTarget();
    }
}
