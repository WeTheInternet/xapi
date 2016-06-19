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
public interface UiService <Element, E extends UiElement<Element, E>> {

  static UiService getUiService() {
    return X_Inject.singleton(UiService.class);
  }

  <Generic extends E> UiBuilder<E> newBuilder(Class<Generic> cls);

  ClassTo<In1Out1<String,Object>> getDeserializers();

  ClassTo<In1Out1<Object,String>> getSerializers();

  UiWithAttributes <Element, E> newAttributes(E e);

  UiWithProperties <Element, E> newProperties(E e);

  Object getHost(Object from);

}
