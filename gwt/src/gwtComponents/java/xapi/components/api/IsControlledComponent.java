package xapi.components.api;

import elemental.dom.Element;
import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * Created by james on 25/10/15.
 */
@JsType
public interface IsControlledComponent
    <E extends Element, W extends IsWebComponent<E>, C extends IsComponentController<E, W>>
    extends IsWebComponent<E>,
    OnWebComponentAttached<E>,
    OnWebComponentCreated<E>,
    OnWebComponentDetached<E>
{

  @JsProperty
  C getController();

  @JsProperty
  void setController(C controller);

  @JsIgnore
  default C getOrMakeController() {
    return getOrMake(this);
  }

  @JsIgnore
  static <E extends Element, W extends IsWebComponent<E>, C extends IsComponentController<E, W>> C getOrMake(IsControlledComponent<E, W, C> from) {
      C c = from.getController();
      if (c == null) {
        c = from.createController();
        from.setController(c);
      }
      return c;
  }

  C createController();

  @Override
  default void onAttached(E element) {
    getOrMakeController().onAttached((W)this);
  }

  @Override
  default void onDetached(E element) {
    getOrMakeController().onDetached((W)this);
  }

  @Override
  default void onCreated(E element) {
    getOrMakeController().onCreated((W)this);
  }

}
