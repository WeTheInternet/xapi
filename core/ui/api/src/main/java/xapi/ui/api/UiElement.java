package xapi.ui.api;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import xapi.collect.api.IntTo;
import xapi.event.api.IsEvent;
import xapi.event.impl.EventTypes;
import xapi.fu.In1Out1;
import xapi.inject.X_Inject;
import xapi.ui.api.event.UiEvent;
import xapi.ui.api.event.UiEventHandler;
import xapi.ui.impl.DelegateElementInjector;
import xapi.ui.service.UiService;

import javax.validation.constraints.NotNull;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/18/16.
 */
@JsType
public interface UiElement
    <Node, Element extends Node, Base extends UiElement<Node, ? extends Node, Base>>
    extends ElementInjector<Node, Base> {

  @JsProperty
  Base getParent();

  @JsProperty
  Base setParent(Base parent);

  @JsIgnore
  IntTo<Base> getChildren();

  @JsIgnore
  String toSource();

  @JsIgnore
  Element element();

  @JsIgnore
  default Node getHost() {
    return (Node)UiService.getUiService().getHost(this);
  }

  @JsIgnore
  <F extends UiFeature, Generic extends F> F getFeature(Class<Generic> cls);

  @JsIgnore
  <F extends UiFeature, Generic extends F> F addFeature(Class<Generic> cls, F feature);

  @JsIgnore
  default <F extends UiFeature, Generic extends F> F getOrAddFeature(Class<Generic> cls, In1Out1<Base, F> factory) {
    F f = getFeature(cls);
    if (f == null) {
      f = factory.io(ui());
      // purposely not null checking.  Will leave this to implementors to enforce.
      f.initialize(this, getUiService());
      addFeature(cls, f);
    }
    return f;
  }

  @JsIgnore
  default UiEventsFeature getEvents() {
    return getOrAddFeature(UiEventsFeature.class, i->createFeature(UiEventsFeature.class));
  }

  @JsIgnore
  default <F extends UiFeature, Generic extends F> F createFeature(Class<Generic> cls) {
    return X_Inject.instance(cls);
  }

  @JsIgnore
  @SuppressWarnings("unchecked")
  default Base ui() {
    return (Base) this;
  }

  @JsIgnore
  default UiWithAttributes<Node, Base> getAttributes() {
    return getOrAddFeature(UiWithAttributes.class, e->getUiService().newAttributes(e));
  }

  @JsIgnore
  default UiWithProperties<Node, Base> getProperties() {
    return getOrAddFeature(UiWithProperties.class, e->getUiService().newProperties(e));
  }

  @JsIgnore
  default UiService<Node, Base> getUiService() {
    return UiService.getUiService();
  }

  @Override
  default void appendChild(Base newChild) {
    asInjector().appendChild(newChild);
    newChild.setParent(ui());
  }

  @Override
  default void removeChild(Base child) {
    asInjector().removeChild(child);
  }

  void insertAdjacent(ElementPosition pos, Base child);

  default void insertBefore(Base newChild) {
    asInjector().insertBefore(newChild);
  }

  default void insertAtBegin(Base newChild) {
    asInjector().insertAtBegin(newChild);
  }

  default void insertAfter(Base newChild) {
    asInjector().insertAfter(newChild);
  }

  default void insertAtEnd(Base newChild) {
    asInjector().insertAtEnd(newChild);
  }

  @JsIgnore
  default ElementInjector<Node, Base> asInjector() {
    // Platforms like Gwt might erase the type information off a
    // raw html / javascript type, so we return "real java objects" here.
    // This also allows implementors to insert control logic to the element attachment methods.
    return new DelegateElementInjector<>(ui());
  }

  @JsIgnore
  default boolean removeFromParent() {
    final Base parent = getParent();
    if (parent != null) {
      parent.removeChild(ui());
      assert getParent() == null : "Parent did not correctly detach child." +
          "\nChild " + toSource()+
          "\nParent " + parent.toSource();
      return true;
    }
    return false;
  }

  @JsIgnore
  default boolean fireEventCapture(@NotNull IsEvent<?> event) {
    return getEvents().fireCapture(event);
  }

  @JsIgnore
  default boolean handlesBubble(@NotNull IsEvent<?> event) {
    return getEvents().fireCapture(event);
  }

  @JsIgnore
  default boolean handlesCapture(@NotNull IsEvent<?> event) {
    return getEvents().fireCapture(event);
  }

  @JsIgnore
  default boolean fireEventBubble(@NotNull IsEvent<?> event) {
    return getEvents().fireBubble(event);
  }

  @JsIgnore
  default <Payload> Base onEventBubble(EventTypes type, UiEventHandler<Payload, Node, Base, ? extends UiEvent<Payload, Node, Base>> handler) {
    getEvents().addBubbling(type, handler);
    return ui();
  }
  @JsIgnore
  default <Payload> Base onEvent(EventTypes type, UiEventHandler<Payload, Node, Base, ? extends UiEvent<Payload, Node, Base>> handler, boolean capture) {
    if (capture) {
      return onEventCapture(type, handler);
    } else {
      return onEventBubble(type, handler);
    }
  }
  @JsIgnore
  default <Payload> Base onEventCapture(EventTypes type, UiEventHandler<Payload, Node, Base, ? extends UiEvent<Payload, Node, Base>> handler) {
    getEvents().addCapturing(type, handler);
    return ui();
  }

  @JsIgnore
  boolean addStyleName(String style);

  @JsIgnore
  boolean removeStyleName(String style);
}
