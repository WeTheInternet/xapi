package xapi.elemental.impl;

import elemental2.dom.Element;
import elemental2.dom.HTMLElement;
import elemental2.dom.Node;
import jsinterop.base.Js;
import jsinterop.base.JsPropertyMap;
import xapi.annotation.inject.SingletonOverride;
import xapi.elemental.api.UiElementGwt;
import xapi.event.api.EventHandler;
import xapi.event.api.IsEventType;
import xapi.fu.In1Out1;
import xapi.fu.In2;
import xapi.platform.GwtPlatform;
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
@SingletonOverride(implFor = UiService.class, priority = 1)
public class Gwt3UiService extends UiServiceImpl <Node, UiElementGwt<?>> {

  public class ElementalAttributes extends UiWithAttributes<Node, UiElementGwt<?>> {
    public ElementalAttributes() {
    }

    @SuppressWarnings("Convert2MethodRef") // gwt dislikes method references to overlay methods
    @Override
    protected In1Out1<String, String> findGetter(UiElementGwt<?> element) {
      final HTMLElement e = element.getElement();
      return name->e.getAttribute(name);
    }

    @Override
    @SuppressWarnings("Convert2MethodRef") // gwt dislikes method references to overlay methods
    protected In2<String, String> findSetter(UiElementGwt<?> element) {
      final HTMLElement e = element.getElement();
      return (name, value)->e.setAttribute(name, value);
    }
  }

  public class ElementalProperties extends UiWithProperties<Node, UiElementGwt<?>> {

    @Override
    protected In1Out1<String, Object> findGetter(UiElementGwt<?> element) {
      final Element ele = element.getElement();
      return Js.asPropertyMap(ele)::get;
    }

    @Override
    protected In2<String, Object> findSetter(UiElementGwt<?> element) {
      final Element ele = element.getElement();
      return Js.asPropertyMap(ele)::set;
    }
  }

  @Override
  public UiWithAttributes<Node, UiElementGwt<?>> newAttributes(UiElementGwt<?> el) {
    final ElementalAttributes ui = new ElementalAttributes();
    ui.initialize(el, this);
    return ui;
  }

  @Override
  public UiWithProperties<Node, UiElementGwt<?>> newProperties(UiElementGwt<?> el) {
      final ElementalProperties ui = new ElementalProperties();
      ui.initialize(el, this);
      return ui ;
  }

  @Override
  public Object getHost(Object from) {
    if (from instanceof UiElementGwt) {
      UiElementGwt<?> e = (UiElementGwt) from;
      final Element host = nativeHost(e.getElement());
      return UiElementGwt.fromWeb(host);
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
    return element.parentNode;
  }

  @Override
  public void bindEvent(
      IsEventType type, UiElementGwt<?> ui, Node element, EventHandler handler, boolean useCapture
  ) {
    element.addEventListener(type.getEventType(), e->{
      final UiEventManager<Node, UiElementGwt<?>> manager = uiEvents();
      manager.fireUiEvent(ui, type, toPayload(type, ui, element, e));
    }, useCapture);
  }

}
