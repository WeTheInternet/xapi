package xapi.ui.api.event;

import xapi.event.api.IsEventType;
import xapi.event.impl.EventTypes;
import xapi.ui.api.UiElement;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/17/16.
 */
public interface UiBlurEvent<Payload, Node, Ui extends UiElement<Node, ? extends Node, Ui>> extends UiEvent<Payload, Node, Ui> {

    @Override
    default IsEventType getType() {
        return EventTypes.Blur;
    }

}
