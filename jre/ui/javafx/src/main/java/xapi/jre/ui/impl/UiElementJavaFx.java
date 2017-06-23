package xapi.jre.ui.impl;

import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
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
  public N getElement() {
    return (N)super.getElement();
  }

  protected ObservableList<Node> getInsertionPoint() {
    final N node = getElement();
    assert node instanceof Pane : "Cannot insert into " + this + " as element " + node + " is not a Pane subclass";
    return getChildren(node);
  }

  @Override
  public void insertAdjacent(ElementPosition pos, UiElementJavaFx child) {
    final Node node = getElement();
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
          children.add(myPos, child.getElement());
        } else {
          children.add(myPos+1, child.getElement());
        }
        child.setParent(parent);
        break;
      case AFTER_BEGIN:
      case BEFORE_END:
        children = getInsertionPoint();
        if (pos == ElementPosition.AFTER_BEGIN) {
          children.add(0, child.getElement());
        } else {
          children.add(children.size(), child.getElement());
        }
        child.setParent(ui());
        break;
      default:
        throw new IllegalStateException("Unhandled injection position " + pos + " in " + this);
    }
  }

  @Override
  public void appendChild(UiElementJavaFx newChild) {
    getInsertionPoint().add(newChild.getElement());
    newChild.setParent(ui());
  }

  @Override
  public void removeChild(UiElementJavaFx child) {
    getInsertionPoint().remove(child.getElement());
    child.setParent(null);
  }

  @Override
  public boolean removeFromParent() {
    final Parent par = getElement().getParent();
    if (par == null) {
      return false;
    }
    if (getChildren(par).remove(getElement())) {
      setParent(null);
      return true;
    }
    return false;
  }

  protected ObservableList<Node> getChildren(Node parent) {
    return getChildList(parent);
  }

  public static ObservableList<Node> getChildList(Node parent) {
    if (parent instanceof Pane) {
      return ((Pane)parent).getChildren();
    } else if (parent instanceof Group) {
      return ((Group)parent).getChildren();
    } else {
      throw new IllegalArgumentException("Cannot get children of parent " + parent);
    }
  }

  @Override
  protected N initialize() {
    try {
      return elementType.newInstance();
    } catch (Exception e) {
      throw X_Debug.rethrow(e);
    }
  }

  public boolean addStyleName(String name) {
    return getElement().getStyleClass().add(name);
  }
  public boolean removeStyleName(String name) {
    return getElement().getStyleClass().remove(name);
  }

}
