package xapi.components.api;

import com.google.gwt.core.client.js.JsProperty;
import com.google.gwt.core.client.js.JsType;

public interface WebComponentFactory<W extends IsWebComponent<?>> {

  /**
   * Generates a web component as defined by the interface contract of the
   * supplied class.
   * <p>
   * You are recommended to use a @{@link JsType} interface, as the generator
   * will bind your supplied default methods (and bean-style @{@link JsProperty}
   * methods) to the generated web component type.
   * <p>
   * Example code:
   * <p>
   * <code>
   * WebComponentFactory<ExampleWebComponent> factory = GWT.create(ExampleWebComponent.class);
   * </code>
   *
   * @return a new instance of the WebComponent this factory is generated for.
   */
  W newComponent();

}
