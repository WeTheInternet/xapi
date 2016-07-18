package xapi.jre.ui.impl;

import javafx.scene.Node;
import xapi.ui.impl.UiServiceImpl;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/17/16.
 */
public class UiServiceJavaFx extends UiServiceImpl <Node, UiElementJavaFx<?>> {

    @Override
    protected Node getParent(Node node) {
        return node.getParent();
    }
}
