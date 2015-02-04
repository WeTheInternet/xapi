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
   * If you do not supply a class to extend via {@link #extendClass()},
   * then the string value of this method will be used to determine the native
   * DOM prototype to extend.  The default is HTMLElement.
   */
  String extendProto() default "HTMLElement";

  /**
   * The tagName of the WebComponent to use.
   */
  String tagName();
}
