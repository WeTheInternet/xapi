package xapi.elemental.api;

import elemental.dom.Element;
import xapi.annotation.inject.InstanceDefault;
import xapi.elemental.X_Elemental;
import xapi.inject.X_Inject;
import xapi.ui.api.ElementPosition;
import xapi.ui.impl.AbstractUiElement;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/19/16.
 */
@InstanceDefault(implFor = UiElementWeb.class)
public class UiElementWeb <E extends Element> extends AbstractUiElement<Element, E, UiElementWeb<E>> {

  public UiElementWeb() {
    super(UiElementWeb.class);
  }

  public static UiElementWeb fromWeb(Element element) {
    UiElementWeb el = X_Inject.instance(UiElementWeb.class);
    el.setElement(element);
    return el;
  }

  @Override
  public void appendChild(UiElementWeb<E> newChild) {
    super.appendChild(newChild);
    final E e = element();
    final E c = newChild.element();
    e.appendChild(c);
  }

  @Override
  public void removeChild(UiElementWeb<E> child) {
    super.removeChild(child);
    final E e = element();
    final E c = child.element();
    e.appendChild(c);
  }

  @Override
  public String toSource() {
    return element().getOuterHTML();
  }

  @Override
  public void insertAdjacent(ElementPosition pos, UiElementWeb<E> child) {
    final E e = element();
    final E c = child.element();
    e.insertAdjacentElement(pos.position(), c);
  }

  @Override
  public boolean addStyleName(String style) {
    return X_Elemental.addClassName(element(), style);
  }

  @Override
  public boolean removeStyleName(String style) {
    return X_Elemental.removeClassName(element(), style);
  }
}
