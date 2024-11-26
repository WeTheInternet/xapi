package xapi.components.api;

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

  /**
   * @return a query selector that can be used to locate elements of the correct instance.
   * This is primarily useful for abstracting away the differences when an element extends
   * an existing DOM element.
   * <p>
   * For example, a custom element which does not extend another element will have a query
   * selector of "my-custom-element", whereas if it extends an anchor, it would have a query
   * selector of "a[is=my-custom-element]".
   */
  default String querySelector() {
    throw new UnsupportedOperationException();
  }
}
