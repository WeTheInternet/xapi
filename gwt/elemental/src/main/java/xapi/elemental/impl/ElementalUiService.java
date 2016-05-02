package xapi.elemental.impl;

import elemental.dom.Element;
import elemental.js.dom.JsElement;
import xapi.annotation.inject.SingletonOverride;
import xapi.elemental.api.UiElementWeb;
import xapi.fu.In1Out1;
import xapi.fu.In2;
import xapi.gwt.collect.JsDictionary;
import xapi.ui.api.UiElement;
import xapi.ui.api.UiWithAttributes;
import xapi.ui.api.UiWithProperties;
import xapi.ui.impl.UiServiceImpl;
import xapi.ui.service.UiService;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/19/16.
 */
@SingletonOverride(implFor = UiService.class)
public class ElementalUiService extends UiServiceImpl {

  public static class ElementalAttributes extends UiWithAttributes<UiElementWeb> {
    public ElementalAttributes(UiElementWeb e) {
      super(e);
    }

    @Override
    protected In1Out1<String, String> findGetter(UiElementWeb element) {
      return element.element()::getAttribute;
    }

    @Override
    protected In2<String, String> findSetter(UiElementWeb element) {
      return element.element()::setAttribute;
    }
  }

  public static class ElementalProperties extends UiWithProperties<UiElementWeb> {
    public ElementalProperties(UiElementWeb e) {
      super(e);
    }

    @Override
    protected In1Out1<String, Object> findGetter(UiElementWeb element) {
      final Element ele = element.element();
      if (ele instanceof JsElement) {
        return ((JsElement) ele).<JsDictionary>cast()::get;
      }
      throw new UnsupportedOperationException();
    }

    @Override
    protected In2<String, Object> findSetter(UiElementWeb element) {
      final Element ele = element.element();
      if (ele instanceof JsElement) {
        return ((JsElement) ele).<JsDictionary>cast()::put;
      }
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public ElementalAttributes newAttributes(UiElement e) {
    return new ElementalAttributes((UiElementWeb) e);
  }

  @Override
  public ElementalProperties newProperties(UiElement e) {
    return new ElementalProperties((UiElementWeb)e);
  }

}
