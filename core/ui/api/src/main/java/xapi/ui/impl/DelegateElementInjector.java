package xapi.ui.impl;

import xapi.ui.api.ElementInjector;
import xapi.ui.api.ElementPosition;
import xapi.ui.api.UiElement;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/18/16.
 */
public class DelegateElementInjector implements ElementInjector {

  // The positional anchor of where to insert items.
  private final UiElement anchor;

  public DelegateElementInjector(UiElement anchor) {
    this.anchor = anchor;
  }

  @Override
  public void appendChild(UiElement newChild) {
    anchor.appendChild(newChild);
  }

  @Override
  public void insertBefore(UiElement newChild) {
    anchor.insertAdjacent(ElementPosition.BEFORE_BEGIN, newChild);
  }

  @Override
  public void insertAtBegin(UiElement newChild) {
    anchor.insertAdjacent(ElementPosition.AFTER_BEGIN, newChild);
  }

  @Override
  public void insertAfter(UiElement newChild) {
    anchor.insertAdjacent(ElementPosition.AFTER_END, newChild);
  }

  @Override
  public void insertAtEnd(UiElement newChild) {
    anchor.insertAdjacent(ElementPosition.BEFORE_END, newChild);
  }

  @Override
  public void removeChild(UiElement child) {
    if (child.getParent() == anchor) {
      anchor.removeChild(child);
    } else {
      assert child.getParent() == null :
          "Attempting to remove a child that is attached to a different parent." +
              "\nElement: " + child.toSource() +
              "\nWrong Parent: " + anchor.toSource() +
              "\nActual Parent: " + child.getParent().toSource() ;
    }
  }
}
