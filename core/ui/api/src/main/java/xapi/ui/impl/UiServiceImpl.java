package xapi.ui.impl;

import xapi.annotation.inject.SingletonDefault;
import xapi.collect.api.ClassTo;
import xapi.except.NotYetImplemented;
import xapi.fu.In1Out1;
import xapi.fu.Out1;
import xapi.inject.X_Inject;
import xapi.ui.api.Ui;
import xapi.ui.api.UiBuilder;
import xapi.ui.api.UiElement;
import xapi.ui.api.UiWithAttributes;
import xapi.ui.api.UiWithProperties;
import xapi.ui.service.UiService;
import xapi.util.X_String;

import static xapi.collect.X_Collect.newClassMap;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/19/16.
 */
@SingletonDefault(implFor = UiService.class)
@SuppressWarnings("unchecked")
public class UiServiceImpl implements UiService {

  private ClassTo<Out1<UiBuilder>> builderFactories;
  private ClassTo<In1Out1<String, Object>> deserializers;
  private ClassTo<In1Out1<Object, String>> serializers;

  public UiServiceImpl() {
    builderFactories = newClassMap(Out1.class);
    deserializers = newClassMap(In1Out1.class);
    serializers = newClassMap(In1Out1.class);
  }

  @Override
  public <E extends UiElement, Generic extends E> UiBuilder<E> newBuilder(Class<Generic> cls) {
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
  public ClassTo<In1Out1<String, Object>> getDeserializers() {
    return deserializers;
  }

  @Override
  public ClassTo<In1Out1<Object, String>> getSerializers() {
    return serializers;
  }

  @Override
  public UiWithAttributes newAttributes(UiElement e) {
    return new UiWithAttributes<>(e);
  }

  @Override
  public UiWithProperties newProperties(UiElement e) {
    return new UiWithProperties<>(e);
  }

  @Override
  public Object getHost(Object from) {
    throw new NotYetImplemented(getClass() + " needs to override .getHost()");
  }
}
