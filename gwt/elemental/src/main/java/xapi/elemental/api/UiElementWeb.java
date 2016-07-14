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
public class UiElementWeb <E extends Element> extends AbstractUiElement<E, UiElementWeb<E>> {

  public UiElementWeb() {
    super(UiElementWeb.class);
  }

  public static UiElementWeb fromWeb(Element element) {
    UiElementWeb el = X_Inject.instance(UiElementWeb.class);
    el.setElement(element);
    return el;
  }

  @Override
  public <El extends UiElement<E, El>> void appendChild(El child) {
    assert child instanceof UiElementWeb : "You may only append web elements to other web elements";
    super.appendChild(child);
    final E e = element();
    final E c = child.element();
    e.appendChild(c);
  }

  @Override
  public <El extends UiElement<E, El>> void removeChild(El child) {
    assert child instanceof UiElementWeb : "You may only remove web elements from other web elements";
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
  public <El extends UiElement<E, El>> void insertAdjacent(ElementPosition pos, El child) {
    assert child instanceof UiElementWeb : "You may only insert web elements into other web elements";
    final E e = element();
    final E c = child.element();
    e.insertAdjacentElement(pos.position(), c);
  }
}
