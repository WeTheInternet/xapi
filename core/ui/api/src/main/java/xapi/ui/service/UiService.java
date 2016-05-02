package xapi.ui.service;

import xapi.collect.api.ClassTo;
import xapi.fu.In1Out1;
import xapi.inject.X_Inject;
import xapi.ui.api.UiBuilder;
import xapi.ui.api.UiElement;
import xapi.ui.api.UiWithAttributes;
import xapi.ui.api.UiWithProperties;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/19/16.
 */
public interface UiService {

  static UiService getUiService() {
    return X_Inject.singleton(UiService.class);
  }

  <Element extends UiElement, Generic extends Element> UiBuilder<Element> newBuilder(Class<Generic> cls);

  ClassTo<In1Out1<String,Object>> getDeserializers();

  ClassTo<In1Out1<Object,String>> getSerializers();

  <E extends UiElement> UiWithAttributes <E> newAttributes(E e);

  <E extends UiElement> UiWithProperties <E> newProperties(E e);
}
