package xapi.elemental.impl;

import elemental.dom.Element;
import elemental.js.dom.JsElement;
import xapi.annotation.inject.SingletonOverride;
import xapi.elemental.api.UiElementWeb;
import xapi.fu.In1Out1;
import xapi.fu.In2;
import xapi.gwt.collect.JsDictionary;
import xapi.ui.api.UiWithAttributes;
import xapi.ui.api.UiWithProperties;
import xapi.ui.impl.UiServiceImpl;
import xapi.ui.service.UiService;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/19/16.
 */
@SingletonOverride(implFor = UiService.class)
public class ElementalUiService extends UiServiceImpl <Element, UiElementWeb<Element>> {

  public static class ElementalAttributes extends UiWithAttributes<Element, UiElementWeb<Element>> {
    public ElementalAttributes(UiElementWeb e) {
      super(e);
    }

    @Override
    protected In1Out1<String, String> findGetter(UiElementWeb<Element> element) {
      return element.element()::getAttribute;
    }

    @Override
    protected In2<String, String> findSetter(UiElementWeb<Element> element) {
      return element.element()::setAttribute;
    }
  }

  public static class ElementalProperties extends UiWithProperties<Element, UiElementWeb<Element>> {
    public ElementalProperties(UiElementWeb e) {
      super(e);
    }

    @Override
    protected In1Out1<String, Object> findGetter(UiElementWeb<Element> element) {
      final Element ele = element.element();
      if (ele instanceof JsElement) {
        return ((JsElement) ele).<JsDictionary>cast()::get;
      }
      throw new UnsupportedOperationException();
    }

    @Override
    protected In2<String, Object> findSetter(UiElementWeb<Element> element) {
      final Element ele = element.element();
      if (ele instanceof JsElement) {
        return ((JsElement) ele).<JsDictionary>cast()::put;
      }
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public UiWithAttributes<Element, UiElementWeb<Element>> newAttributes(UiElementWeb<Element> el) {
    return new ElementalAttributes(el);
  }

  @Override
  public UiWithProperties<Element, UiElementWeb<Element>> newProperties(UiElementWeb<Element> el) {
      return new ElementalProperties(el);
  }

  @Override
  public Object getHost(Object from) {
    if (from instanceof UiElementWeb) {
      UiElementWeb<?> e = (UiElementWeb) from;
      final Element host = nativeHost(e.element());
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
}
