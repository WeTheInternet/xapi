package xapi.jre.ui.impl;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import xapi.ui.api.ElementPosition;
import xapi.ui.impl.AbstractUiElement;

/**
 * Created by james on 6/7/16.
 */
public class UiElementJavaFx<N extends Node> extends AbstractUiElement<N, UiElementJavaFx<N>> {

  public UiElementJavaFx() {
    super(UiElementJavaFx.class);
  }

  @Override
  public <El extends UiElementJavaFx<N>> void insertAdjacent(ElementPosition pos, El child) {
    final N node = element();
    final ObservableList<Node> children;
    if (node instanceof Pane) {
      Pane p = (Pane) node;
      children = p.getChildren();
    } else {
      throw new IllegalStateException("Cannot append children to " + node + " of type " + getClass());
    }
    switch (pos) {
      case AFTER_BEGIN:
      case AFTER_END:
      case BEFORE_BEGIN:
      case BEFORE_END:
    }
  }
}
