package xapi.jre.ui.impl;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import xapi.ui.api.components.Window;

/**
 * Created by james on 6/7/16.
 */
public class WindowJavaFx extends UiElementJavaFx<BorderPane> implements Window<Node, BorderPane, UiElementJavaFx<?>> {

  public WindowJavaFx() {
    super(BorderPane.class, WindowJavaFx.class);
  }
}
