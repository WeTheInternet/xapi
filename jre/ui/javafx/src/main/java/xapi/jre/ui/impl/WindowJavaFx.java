package xapi.jre.ui.impl;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import xapi.ui.api.components.Window;

/**
 * Created by james on 6/7/16.
 */
public class WindowJavaFx extends UiElementJavaFx<BorderPane, WindowJavaFx> implements Window<Node, BorderPane, WindowJavaFx> {

  public WindowJavaFx() {
    super(BorderPane.class, WindowJavaFx.class);
  }
}
