package xapi.elemental.impl;

import elemental.dom.Element;
import elemental.dom.Node;
import elemental.js.dom.JsElement;
import xapi.annotation.inject.SingletonOverride;
import xapi.elemental.api.PotentialNode;
import xapi.elemental.api.UiElementWeb;
import xapi.event.api.EventHandler;
import xapi.event.api.IsEventType;
import xapi.fu.In1Out1;
import xapi.fu.In2;
import xapi.gwt.collect.JsDictionary;
import xapi.platform.GwtPlatform;
import xapi.ui.api.ElementBuilder;
import xapi.ui.api.UiWithAttributes;
import xapi.ui.api.UiWithProperties;
import xapi.ui.api.event.UiEventManager;
import xapi.ui.impl.UiServiceImpl;
import xapi.ui.service.UiService;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/19/16.
 */
@GwtPlatform
@SingletonOverride(implFor = UiService.class)
public class ElementalUiService extends UiServiceImpl <Node, UiElementWeb<Element>> {

  public class ElementalAttributes extends UiWithAttributes<Node, UiElementWeb<Element>> {
    public ElementalAttributes() {
    }

    @Override
    protected In1Out1<String, String> findGetter(UiElementWeb<Element> element) {
      return element.getElement()::getAttribute;
    }

    @Override
    protected In2<String, String> findSetter(UiElementWeb<Element> element) {
      return element.getElement()::setAttribute;
    }
  }

  public class ElementalProperties extends UiWithProperties<Node, UiElementWeb<Element>> {

    @Override
    protected In1Out1<String, Object> findGetter(UiElementWeb<Element> element) {
      final Element ele = element.getElement();
      if (ele instanceof JsElement) {
        return ((JsElement) ele).<JsDictionary>cast()::get;
      }
      throw new UnsupportedOperationException();
    }

    @Override
    protected In2<String, Object> findSetter(UiElementWeb<Element> element) {
      final Element ele = element.getElement();
      if (ele instanceof JsElement) {
        return ((JsElement) ele).<JsDictionary>cast()::put;
      }
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public UiWithAttributes<Node, UiElementWeb<Element>> newAttributes(UiElementWeb<Element> el) {
    final ElementalAttributes ui = new ElementalAttributes();
    ui.initialize(el, this);
    return ui;
  }

  @Override
  public UiWithProperties<Node, UiElementWeb<Element>> newProperties(UiElementWeb<Element> el) {
      final ElementalProperties ui = new ElementalProperties();
      ui.initialize(el, this);
      return ui ;
  }

  @Override
  public Object getHost(Object from) {
    if (from instanceof UiElementWeb) {
      UiElementWeb<?> e = (UiElementWeb) from;
      final Element host = nativeHost(e.getElement());
      return UiElementWeb.fromWeb(host);
    }
    assert from instanceof Element || isElement(from);
    return nativeHost ((Element) from);
  }

  private native boolean isElement(Object from)
  /*-{
    return from && from.nodeType === 1;
  }-*/;

  private native Element nativeHost(Element element)
  /*-{
    return element && element.host || element;
  }-*/;

  @Override
  protected Node getParent(Node element) {
    return element.getParentNode(); // TODO consider stopping at document.body/head?
  }

  @Override
  public void bindEvent(
      IsEventType type, UiElementWeb<Element> ui, Node element, EventHandler handler, boolean useCapture
  ) {
    element.addEventListener(type.getEventType(), e->{
      final UiEventManager<Node, UiElementWeb<Element>> manager = uiEvents();
      manager.fireUiEvent(ui, type, toPayload(type, ui, element, e));
    }, useCapture);
  }

  @Override
  public ElementBuilder<? extends Node> newBuilder(boolean searchable) {
    return new PotentialNode<>(searchable);
  }
}
