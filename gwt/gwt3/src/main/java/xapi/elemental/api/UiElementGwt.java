package xapi.elemental.api;

import elemental2.core.Function;
import elemental2.core.Reflect;
import elemental2.dom.DomGlobal;
import elemental2.dom.Element;
import elemental2.dom.HTMLAnchorElement;
import elemental2.dom.HTMLDivElement;
import elemental2.dom.HTMLElement;
import elemental2.dom.HTMLLabelElement;
import elemental2.dom.Node;
import jsinterop.base.Js;
import xapi.annotation.inject.InstanceDefault;
import xapi.gwt.api.JsLazyExpando;
import xapi.inject.X_Inject;
import xapi.ui.api.UiInjector;
import xapi.ui.api.ElementPosition;
import xapi.ui.impl.AbstractUiElement;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/19/16.
 */
@InstanceDefault(implFor = UiElementGwt.class)
public class UiElementGwt<E extends HTMLElement>
    extends AbstractUiElement<Node, E, UiElementGwt<?>> {


  private static final String MEMOIZE_KEY = "xapi-element";
  private static final JsLazyExpando<Element, UiElementGwt> expando = new JsLazyExpando<>(MEMOIZE_KEY);
  protected static Function insertAdjacentElement = Js.uncheckedCast(
      Reflect.get(
          Js.uncheckedCast(htmlElementPrototype()),
          "insertAdjacentElement")
  );

  public static native JavaScriptObject htmlElementPrototype()
  /*-{
    return Object.create($wnd.HTMLElement.prototype);
  }-*/;

  public UiElementGwt() {
    super(UiElementGwt.class);
  }


  public static HTMLDivElement newDiv() {
    return Js.uncheckedCast(DomGlobal.document.createElement("div"));
  }

  public static HTMLLabelElement newLabel() {
    return Js.uncheckedCast(DomGlobal.document.createElement("label"));
  }

  public static HTMLAnchorElement newAnchor() {
    return Js.uncheckedCast(DomGlobal.document.createElement("a"));
  }

  public static UiElementGwt<?> fromWeb(Element element) {
    final UiElementGwt result = expando.io(element, e -> {
      final UiElementGwt newEl = newUiElement(e);
      newEl.setElement(element);
      return newEl;
    });
    return result;
  }

  private static UiElementGwt<?> newUiElement(Element e) {
    // TODO: have registered providers per tag name
    return X_Inject.instance(UiElementGwt.class);
  }

  @Override
  public void appendChild(UiElementGwt<?> newChild) {
    newChild.setParent(this);
    final E e = getElement();
    final Element c = newChild.getElement();
    e.appendChild(c);
  }

  @Override
  public void removeChild(UiElementGwt<?> child) {
    assert child.getParent() == this;
    child.setParent(null);
    final E e = getElement();
    final Element c = child.getElement();
    e.appendChild(c);
  }

  @Override
  public String toSource() {
    return (String) Js.asPropertyMap(getElement()).get("outerHTML");
  }

  @Override
  public void insertAdjacent(ElementPosition pos, UiElementGwt<?> child) {
    final E e = getElement();
    final Element c = child.getElement();
    insertAdjacentElement.call(e, pos.position(), c);
    child.setParent(this);
  }

  @Override
  public void insertBefore(UiElementGwt<?> newChild, UiElementGwt<?> refChild) {
    final E e = getElement();
    final Element c = newChild.getElement();
    final Element r = refChild.getElement();
    e.insertBefore(c, r);
    refChild.setParent(this);
  }

  @Override
  public boolean addStyleName(String style) {
    return addClassName(getElement(), style);
  }

  public static boolean addClassName(final Element e, final String cls) {
    if (!hasClassName(e, cls)) {
      e.className = e.className + " " + cls;
      return true;
    }
    return false;
  }


  public static boolean removeClassName(final Element e, final String cls) {
    if (hasClassName(e, cls)) {
      final String clsName = " " + e.className + " ";
      e.className = clsName.replace(" " + cls + " ", " ").trim();
      return true;
    }
    return false;
  }

  public static boolean hasClassName(final Element e, final String cls) {
    return (" " + e.className + " ")
        .contains(" " + cls + " ");
  }

  @Override
  public boolean removeStyleName(String style) {
    return removeClassName(getElement(), style);
  }

  @Override
  public UiInjector<Node, UiElementGwt<?>> asInjector() {
    return new ElementalUiInjector(getElement());
  }
}
