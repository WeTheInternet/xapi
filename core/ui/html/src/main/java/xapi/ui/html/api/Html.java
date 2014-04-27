package xapi.ui.html.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Html {

  String document() default "body";
  
  Css[] css() default {};
  
  El[] body() default {};
  
  /**
   * @return Any other class with a template, in the form of an @Html annotation that you wish to reuse.
   */
  Class<?>[] templates() default {};
  
  String[] renderOrder() default {};
}
