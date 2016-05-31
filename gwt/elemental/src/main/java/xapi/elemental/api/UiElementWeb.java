package xapi.elemental.api;

import elemental.dom.Element;
import xapi.annotation.inject.InstanceDefault;
import xapi.inject.X_Inject;
import xapi.ui.api.ElementPosition;
import xapi.ui.api.UiElement;
import xapi.ui.impl.AbstractUiElement;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/19/16.
 */
@InstanceDefault(implFor = UiElementWeb.class)
public class UiElementWeb extends AbstractUiElement {

  public static UiElementWeb fromWeb(Element element) {
    UiElementWeb el = X_Inject.instance(UiElementWeb.class);
    el.setElement(element);
    return el;
  }

  private void setElement(Element element) {
    this.element = element;
  }

  private Element element;

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
