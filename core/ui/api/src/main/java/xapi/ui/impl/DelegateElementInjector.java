package xapi.ui.impl;

import xapi.ui.api.ElementInjector;
import xapi.ui.api.ElementPosition;
import xapi.ui.api.UiElement;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/18/16.
 */
public class DelegateElementInjector <Node, Base extends UiElement<Node, ? extends Node, Base>> implements ElementInjector <Node, Base> {

  // The positional anchor of where to insert items.
  private final Base anchor;

  public DelegateElementInjector(Base anchor) {
    this.anchor = anchor;
  }

  @Override
  public void appendChild(Base newChild) {
    anchor.appendChild(newChild);
    newChild.setParent(anchor);
  }

  @Override
  public void insertBefore(Base newChild) {
    anchor.insertAdjacent(ElementPosition.BEFORE_BEGIN, newChild);
    newChild.setParent(anchor.getParent());
  }

  @Override
  public void insertAtBegin(Base newChild) {
    anchor.insertAdjacent(ElementPosition.AFTER_BEGIN, newChild);
    newChild.setParent(anchor);
  }

  @Override
  public void insertAfter(Base newChild) {
    anchor.insertAdjacent(ElementPosition.AFTER_END, newChild);
    newChild.setParent(anchor);
  }

  @Override
  public void insertAtEnd(Base newChild) {
    anchor.insertAdjacent(ElementPosition.BEFORE_END, newChild);
    newChild.setParent(anchor.getParent());
  }

  @Override
  public void removeChild(Base child) {
    if (child.getParent() == anchor) {
      anchor.removeChild(child);
      child.setParent(null);
    } else {
      assert child.getParent() == null :
          "Attempting to remove a child that is attached to a different parent." +
              "\nElement: " + child.toSource() +
              "\nWrong Parent: " + anchor.toSource() +
              "\nActual Parent: " + child.getParent().toSource() ;
    }
  }
}
