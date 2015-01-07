package xapi.ui.html.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Html {

  // We should be able to do == checks on this string for defaults,
  String ROOT_ELEMENT = "x-root";

  String document() default ROOT_ELEMENT;

  Css[] css() default {};

  El[] body() default {};

  /**
   * @return Any other class with {@link Html} tags to inherit,
   * or a raw string template to use.
   *
   */
  HtmlTemplate[] templates() default {};

  /**
   * @return the order in which to render children;
   * unspecified means the children will be rendered in the order declared.
   */
  String[] renderOrder() default {};

  /**
   * @return false to treat the template as a static singleton (one and only one instance is used);
   * return true (default) to create a new element on every invocation.
   */
  boolean isDynamic() default true;
}
