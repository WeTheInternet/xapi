package xapi.jre.ui.impl;

import javafx.scene.Node;
import xapi.annotation.inject.SingletonOverride;
import xapi.event.api.EventHandler;
import xapi.event.api.IsEventType;
import xapi.event.impl.EventTypes;
import xapi.except.NotImplemented;
import xapi.platform.JrePlatform;
import xapi.ui.impl.UiServiceImpl;
import xapi.ui.service.UiService;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/17/16.
 */
@SingletonOverride(implFor = UiService.class)
public class UiServiceJavaFx extends UiServiceImpl <Node, UiElementJavaFx<?>> {

    @Override
    protected Node getParent(Node node) {
        return node.getParent();
    }

    @Override
    public void bindEvent(
        IsEventType type, UiElementJavaFx<?> ui, Node node, EventHandler handler, boolean useCapture
    ) {
        if (type instanceof EventTypes) {
            switch ((EventTypes)type) {
                case Click:
                    node.setOnMouseClicked(e->
                        uiEvents().fireUiEvent(ui, type, toPayload(type, ui, node, e))
                    );
                    return;
            }
        }
        throw new NotImplemented(getClass() + " cannot bind event of type " + type);
    }
}
