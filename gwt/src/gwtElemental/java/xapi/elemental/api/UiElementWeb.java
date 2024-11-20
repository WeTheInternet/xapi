package xapi.elemental.api;

import elemental.dom.Element;
import elemental.dom.Node;
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
public class UiElementWeb <E extends Element>
    extends AbstractUiElement<Node, E, UiElementWeb<E>> {

  private static final String MEMOIZE_KEY = "xapi-element";

  public UiElementWeb() {
    super(UiElementWeb.class);
  }

  public static UiElementWeb fromWeb(Element element) {
    final Object existing = element.getDataset().at(MEMOIZE_KEY);
    if (existing != null) {
      return (UiElementWeb) existing;
    }
    UiElementWeb el = X_Inject.instance(UiElementWeb.class);
    el.setElement(element);
    element.getDataset().setAt(MEMOIZE_KEY, el);
    return el;
  }

  @Override
  public String toSource() {
    return getElement().getOuterHTML();
  }

  @Override
  public void insertAdjacent(ElementPosition pos, UiElementWeb<E> child) {
    final E e = getElement();
    final E c = child.getElement();
    e.insertAdjacentElement(pos.position(), c);
  }

  @Override
  public boolean addStyleName(String style) {
    return X_Elemental.addClassName(getElement(), style);
  }

  @Override
  public boolean removeStyleName(String style) {
    return X_Elemental.removeClassName(getElement(), style);
  }
}
