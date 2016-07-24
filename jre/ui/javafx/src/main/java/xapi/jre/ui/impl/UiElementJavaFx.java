package xapi.jre.ui.impl;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import xapi.ui.api.ElementPosition;
import xapi.ui.impl.AbstractUiElement;
import xapi.util.X_Debug;

/**
 * Created by james on 6/7/16.
 */
public class UiElementJavaFx<N extends Node> extends AbstractUiElement<Node, N, UiElementJavaFx<?>> {

  private final Class<N> elementType;

  public <Ui extends UiElementJavaFx<N>> UiElementJavaFx(Class<N> elType, Class<Ui> cls) {
    super(cls);
    this.elementType = elType;
  }

  @Override
  public N element() {
    return (N)super.element();
  }

  protected ObservableList<Node> getInsertionPoint() {
    final N node = element();
    assert node instanceof Pane : "Cannot insert into " + this + " as element " + node + " is not a Pane subclass";
    return ((Pane)node).getChildren();
  }

  @Override
  public void insertAdjacent(ElementPosition pos, UiElementJavaFx child) {
    final Node node = element();
    final ObservableList<Node> children;
    switch (pos) {
      // See https://developer.mozilla.org/en-US/docs/Web/API/Element/insertAdjacentElement for API we are emulating

      // These two cases insert the new node before/after this element.
      // Thus, we must be attached to a parent, and insert into it's children nodes
      case BEFORE_BEGIN:
      case AFTER_END:
        final UiElementJavaFx parent = getParent();
        children = parent.getInsertionPoint();
        final int myPos = children.indexOf(node);
        assert myPos != -1 : "Trying to insert a child adjacent to a node that is not in its parent's insertion point";
        if (pos == ElementPosition.BEFORE_BEGIN) {
          children.add(myPos, child.element());
        } else {
          children.add(myPos+1, child.element());
        }
        child.setParent(parent);
        break;
      case AFTER_BEGIN:
      case BEFORE_END:
        children = getInsertionPoint();
        if (pos == ElementPosition.AFTER_BEGIN) {
          children.add(0, child.element());
        } else {
          children.add(children.size()-1, child.element());
        }
        child.setParent(ui());
        break;
      default:
        throw new IllegalStateException("Unhandled injection position " + pos + " in " + this);
    }
  }

  @Override
  public void appendChild(UiElementJavaFx newChild) {
    getInsertionPoint().add(newChild.element());
    newChild.setParent(ui());
  }

  @Override
  public void removeChild(UiElementJavaFx child) {
    getInsertionPoint().remove(child.element());
    child.setParent(null);
  }

  @Override
  public boolean removeFromParent() {
    if (((Pane)element().getParent()).getChildren().remove(element())) {
      setParent(null);
      return true;
    }
    return false;
  }

  @Override
  public N initialize() {
    try {
      return elementType.newInstance();
    } catch (Exception e) {
      throw X_Debug.rethrow(e);
    }
  }
}
