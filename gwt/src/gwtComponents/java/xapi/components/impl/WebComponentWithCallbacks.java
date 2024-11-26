/**
 *
 */
package xapi.components.impl;

import elemental.dom.Element;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import xapi.components.api.IsWebComponent;
import xapi.components.api.OnWebComponentCreated;

import static xapi.components.impl.JsFunctionSupport.mergeConsumer;

import java.util.function.Consumer;

/**
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
@JsType
public interface WebComponentWithCallbacks <E extends Element> extends
IsWebComponent<E>,
OnWebComponentCreated<E>{

  @Override
  default void onCreated(final Element element) {
    final Consumer<Element> callback = getAfterCreated();
    if (callback != null) {
      callback.accept(element);
    }
  }

  @JsProperty
  Consumer<Element> getAfterCreated();

  @JsProperty
  void setAfterCreated(Consumer<Element> callback);

  default void onAfterCreated(final Consumer<Element> callback, final boolean prepend) {
    final Consumer<Element> existing = getAfterCreated();
    if (existing == null) {
      setAfterCreated(callback);
    } else if (prepend){
      setAfterCreated(mergeConsumer(callback, existing));
    } else {
      setAfterCreated(mergeConsumer(existing, callback));
    }
  }

}
