/**
 *
 */
package xapi.components.impl;

import static xapi.components.impl.JsFunctionSupport.mergeConsumer;

import java.util.function.Consumer;

import com.google.gwt.core.client.js.JsProperty;

import elemental.dom.Element;

import xapi.components.api.IsWebComponent;
import xapi.components.api.OnWebComponentCreated;

/**
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public interface WebComponentWithCallbacks <E extends Element> extends
IsWebComponent<E>,
OnWebComponentCreated<E>{

  @Override
  default void onCreated(Element element) {
    Consumer<Element> callback = afterCreated();
    if (callback != null) {
      callback.accept(element);
    }
  }

  @JsProperty
  Consumer<Element> afterCreated();

  @JsProperty
  void afterCreated(Consumer<Element> callback);

  default void onAfterCreated(Consumer<Element> callback, boolean prepend) {
    Consumer<Element> existing = afterCreated();
    if (existing == null) {
      afterCreated(callback);
    } else if (prepend){
      afterCreated(mergeConsumer(callback, existing));
    } else {
      afterCreated(mergeConsumer(existing, callback));
    }
  }

}
