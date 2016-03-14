package xapi.components.api;

public @interface WebComponent {

  /**
   * If you supply a class to extend (that is not {@link WebComponent}.class),
   * then this prototype will explicitly extend the prototype builder created
   * for the class you are extending.  The extended class will implicitly have
   * WebComponentFactory<Type> factory = GWT.create(Type.class) called.
   */
  Class<?> extendClass() default WebComponent.class;

  /**
   * If you do not supply a class to extend via {@link #extendClass()}, then the
   * string value of this method will be used to determine the native DOM
   * prototype to extend. The default is HTMLElement. <br/>
   * If you wish to extend any other element, the first String should be the
   * name of the prototype to extend, and the second argument should be the
   * tagname of the element. Example: {"HTMLAnchorElement", "a"}
   */
  String[] extendProto() default {
    "HTMLElement"
  };

  ShadowDom[] shadowDom() default {};

  /**
   * The tagName of the WebComponent to use.
   */
  String tagName();

  /**
   * @return any {@link ShadowDomPlugin}s that you want to have X_Inject'd, and applied to your shadow dom.
   * Read the javadoc on {@link ShadowDomPlugin} for more information.
   */
  Class<? extends ShadowDomPlugin>[] plugins() default {};


}
