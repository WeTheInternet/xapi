package xapi.ui.html.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Html {

  Css[] css() default {};
  
  El[] elements() default {};
  
  /**
   * @return Any other class with a template, in the form of an @Html annotation that you wish to reuse.
   */
  Class<?>[] templates() default {};
  
}
