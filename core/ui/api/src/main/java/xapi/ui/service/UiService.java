package xapi.ui.service;

import xapi.collect.api.ClassTo;
import xapi.event.api.EventHandler;
import xapi.event.api.EventManager;
import xapi.event.api.IsEventType;
import xapi.fu.In1Out1;
import xapi.inject.X_Inject;
import xapi.ui.api.UiBuilder;
import xapi.ui.api.UiElement;
import xapi.ui.api.event.UiEventManager;
import xapi.ui.api.UiWithAttributes;
import xapi.ui.api.UiWithProperties;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/19/16.
 */
public interface UiService <Node, E extends UiElement<Node, ? extends Node,  E>> {

  @SuppressWarnings("unchecked")
  static <Node, E extends UiElement<Node, ? extends Node, E>> UiService<Node, E> getUiService() {
    return X_Inject.singleton(UiService.class);
  }

  <Generic extends E> UiBuilder<E> newBuilder(Class<Generic> cls);

  ClassTo<In1Out1<String,Object>> getDeserializers();

  ClassTo<In1Out1<Object,String>> getSerializers();

  UiWithAttributes <Node, E> newAttributes(E e);

  UiWithProperties <Node, E> newProperties(E e);

  Object getHost(Object from);

  EventManager newEventManager();

  UiEventManager<Node, E> uiEvents();

  E findContainer(Node nativeNode);

  IsEventType convertType(String nativeType);

  E bindNode(Node node, E container);

  String debugDump();

  default void bindEvent(IsEventType type, E ui, EventHandler handler, boolean useCapture) {
    bindEvent(type, ui, ui.element(), handler, useCapture);
  }

  void bindEvent(IsEventType type, E ui, Node node, EventHandler handler, boolean useCapture);
}
