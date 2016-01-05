package xapi.components.api;

import elemental.dom.Element;

import com.google.gwt.core.client.js.JsProperty;
import com.google.gwt.core.client.js.JsType;

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

  @Override
  default E element() {
    return IsWebComponent.super.element();
  }

  @JsProperty
  C getController();

  @JsProperty
  void setController(C controller);

  default C getOrMakeController() {
      C c = getController();
      if (c == null) {
        c = createController();
        setController(c);
      }
      return c;
  }

  default C createController() {
    throw new UnsupportedOperationException();
  }

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
