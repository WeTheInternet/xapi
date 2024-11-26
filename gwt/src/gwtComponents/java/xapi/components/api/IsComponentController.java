package xapi.components.api;

import elemental.dom.Element;

/**
 * Created by james on 25/10/15.
 */
public interface IsComponentController <E extends Element, C extends IsWebComponent<E>> extends HasElement<E> {

  @Override
  default E element() {
    return getComponent().element();
  }

  C getComponent();

  default void onAttached(C wtiReference) {}
  default void onDetached(C wtiReference) {}
  default void onCreated(C wtiReference) {}

}
