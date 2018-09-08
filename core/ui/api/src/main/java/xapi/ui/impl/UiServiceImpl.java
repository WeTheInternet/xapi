package xapi.ui.impl;

import xapi.annotation.inject.SingletonDefault;
import xapi.collect.api.ClassTo;
import xapi.event.api.EventHandler;
import xapi.event.api.EventManager;
import xapi.event.api.IsEventType;
import xapi.event.impl.EventTypes;
import xapi.except.NotImplemented;
import xapi.except.NotYetImplemented;
import xapi.fu.In1Out1;
import xapi.fu.Out1;
import xapi.inject.X_Inject;
import xapi.ui.api.*;
import xapi.ui.api.event.UiEventManager;
import xapi.ui.service.UiService;
import xapi.util.X_Debug;
import xapi.util.X_String;

import java.util.WeakHashMap;

import static xapi.collect.X_Collect.newClassMap;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/19/16.
 */
@SingletonDefault(implFor = UiService.class)
@SuppressWarnings("unchecked")
public class UiServiceImpl <Node, E extends UiElement<Node, ? extends Node, E>>
    implements UiService <Node, E> {

  private ClassTo<Out1<UiBuilder>> builderFactories;
  private ClassTo<In1Out1<String, Object>> deserializers;
  private ClassTo<In1Out1<Object, String>> serializers;
  private final UiEventManager<Node, E> uiEvents;
  private final WeakHashMap<Node, E> knownNodes;

  public UiServiceImpl() {
    builderFactories = newClassMap(Out1.class);
    deserializers = newClassMap(In1Out1.class);
    serializers = newClassMap(In1Out1.class);
    uiEvents = createUiEventManager();
    knownNodes = new WeakHashMap<>();
  }

  @Override
  public String debugDump() {
    StringBuilder b = new StringBuilder();
    b .append("KnownNode: ").append(knownNodes.toString().replace(',', '\n'))
      .append("\nDeserializers: ").append(deserializers)
      .append("\nSerializers: ").append(serializers)
      .append("\nBuilders: ").append(builderFactories);
    return b.toString();
  }

  protected UiEventManager<Node,E> createUiEventManager() {
    return new UiEventManager<>(this);
  }

  @Override
  public UiEventManager<Node, E> uiEvents() {
    return uiEvents;
  }

  @Override
  public <Generic extends E> UiBuilder<E> newBuilder(Class<Generic> cls) {
    final Out1<UiBuilder> factory = builderFactories.getOrCompute(cls,
        key -> () -> {
      UiBuilder<E> builder;
      try {
        builder = X_Inject.instance(UiBuilder.class);
      } catch (Throwable ignored) {
        builder = new UiBuilder<E>() {
          @Override
          protected E instantiate() {
            return X_Inject.instance(cls);
          }
        };
      }
      return builder;
    });
    final UiBuilder<E> builder = factory.out1();
    Ui ui = cls.getAnnotation(Ui.class);
    if (ui != null) {
      if (!ui.type().isEmpty()) {
        builder.setType(ui.type());
      }
      if (X_String.isNotEmpty(ui.value())) {
        builder.setSource(ui.value());
      }
    }
    return builder;

  }

  @Override
  public ElementBuilder<? extends Node> newBuilder(boolean searchable) {
    assert false : getClass() + " must implement newBuilder(boolean)";
    throw X_Debug.recommendAssertions();
  }

  @Override
  public ClassTo<In1Out1<String, Object>> getDeserializers() {
    return deserializers;
  }

  @Override
  public ClassTo<In1Out1<Object, String>> getSerializers() {
    return serializers;
  }

  @Override
  public UiWithAttributes<Node, E> newAttributes(E e) {
    final UiWithAttributes<Node, E> ui = new UiWithAttributes<>();
    ui.initialize(e, this);
    return ui;
  }

  @Override
  public UiWithProperties<Node, E> newProperties(E e) {
    UiWithProperties ui = new UiWithProperties<>();
    ui.initialize(e, this);
    return ui;
  }

  @Override
  public Object getHost(Object from) {
    throw new NotYetImplemented(getClass() + " needs to override .getHost()");
  }

  @Override
  public EventManager newEventManager() {
    return new EventManager();
  }

  protected Node getParent(Node node) {
    throw new NotImplemented(getClass() + " must implement .getParent()");
  }

  @Override
  public E findContainer(Node nativeNode) {
    E container = knownNodes.get(nativeNode);
    while (container == null) {
      nativeNode = getParent(nativeNode);
      container = knownNodes.get(nativeNode);
    }
    return container;
  }

  @Override
  public IsEventType convertType(String nativeType) {
    return EventTypes.convertType(nativeType);
  }

  @Override
  public E bindNode(Node node, E container) {
    knownNodes.put(node, container);
    return container;
  }

  @Override
  public void bindEvent(IsEventType type, E ui, Node node, EventHandler handler, boolean useCapture) {
    throw new NotImplemented(getClass() + " must implement .bindEvent");
  }

  protected <Payload, NativeEvent> Payload toPayload(IsEventType type, E ui, Node element, NativeEvent e) {
    return null;
  }
}
