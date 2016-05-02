package xapi.elemental.api;

import elemental.dom.Element;
import xapi.ui.api.ElementPosition;
import xapi.ui.api.UiElement;
import xapi.ui.impl.AbstractUiElement;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/19/16.
 */
public class UiElementWeb extends AbstractUiElement {

  protected Element element;

  public Element element() {
    return element;
  }

  @Override
  public void appendChild(UiElement child) {
    assert child instanceof UiElementWeb : "You may only append web elements to other web elements";
    super.appendChild(child);
    element.appendChild(((UiElementWeb)child).element);
  }

  @Override
  public void removeChild(UiElement child) {
    assert child instanceof UiElementWeb : "You may only remove web elements from other web elements";
    super.removeChild(child);
    element.removeChild(((UiElementWeb)child).element);
  }

  @Override
  public String toSource() {
    return element.getOuterHTML();
  }

  @Override
  public void insertAdjacent(ElementPosition position, UiElement child) {
    assert child instanceof UiElementWeb : "You may only insert web elements into other web elements";
    element.insertAdjacentElement(position.position(), ((UiElementWeb)child).element);
  }
}
