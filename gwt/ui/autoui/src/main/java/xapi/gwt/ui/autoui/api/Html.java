package xapi.gwt.ui.autoui.api;

public @interface Html {

  Css[] css() default {};
  
  El[] elements() default {};
  
  /**
   * @return Any other class with a template, in the form of an @Html annotation that you wish to reuse.
   */
  Class<?>[] templates() default {};
  
}
