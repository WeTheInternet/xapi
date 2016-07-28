package xapi.jre.ui.impl;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.Window;
import xapi.annotation.inject.SingletonOverride;
import xapi.event.api.EventHandler;
import xapi.event.api.IsEventType;
import xapi.event.impl.EventTypes;
import xapi.except.NotImplemented;
import xapi.fu.Pointer;
import xapi.ui.impl.UiServiceImpl;
import xapi.ui.service.UiService;

import static xapi.fu.Pointer.pointer;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/17/16.
 */
@SingletonOverride(implFor = UiService.class)
public class UiServiceJavaFx extends UiServiceImpl <Node, UiElementJavaFx<?>> {

    public static <T extends Pane> T setHeight(T node, double height) {
        node.setMaxHeight(height);
        node.setMinHeight(height);
        node.setPrefHeight(height);
        return node;
    }

    public static <T extends Pane> T setWidth(T node, double width) {
        node.setMaxWidth(width);
        node.setMinWidth(width);
        node.setPrefWidth(width);
        return node;
    }

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

    public static <T extends Node> T withHBoxMargin(T node, double margin) {
        HBox.setMargin(node, new Insets(margin));
        return node;
    }

    public static <T extends Node> T withHBoxMargin(T node, double top, double right, double bottom, double left) {
        HBox.setMargin(node, new Insets(top, right, bottom, left));
        return node;
    }

    public static Button newButton(String text, javafx.event.EventHandler<ActionEvent> o) {
        Button b = new Button(text);
        b.setOnAction(o);
        return b;
    }

    public static void moveWindowOnDrag(Node toolBar, Window stage) {
        final Pointer<Double> x = pointer(), y = pointer();
        toolBar.setOnMousePressed(e -> {
            // record a delta distance for the drag and drop operation.
            x.in(stage.getX() - e.getScreenX());
            y.in(stage.getY() - e.getScreenY());
        });
        toolBar.setOnMouseDragged(e->{
            stage.setX(e.getScreenX() + x.out1());
            stage.setY(e.getScreenY() + y.out1());
        });
    }
}
