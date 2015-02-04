package xapi.components.api;

public interface WebComponentBinder <W extends IsWebComponent<?>, I extends W> {

  /**
   * Generate and create a web component that internally forwards all calls to
   * the supplied implementation.
   * <br/>
   * If the underlying web component type has already been registered,
   * this will create an element that
   * has any methods implemented by webComponentClass overridden
   * (by manually defining the methods on the element).
   *
   * @param webComponentClass
   * @param webComponentModel
   * @return
   */
  W bindWebComponent(Class<I> webComponentClass, I webComponentModel);

}
