package xapi.ui.autoui.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.PACKAGE})
@Retention(RetentionPolicy.RUNTIME)
public @interface UiOptions {

  /**
   * @return A list of classes to import; this allows you to reuse a rendering setup in multiple places.
   * <br>
   * This option is not currently supported in all runtimes.
   */
  Class<?>[] imports() default {};
  
  /**
   * @return the names of all fields to use; java-bean method names will have any "get" or "is" prefix
   * removed (so, getValue -> "value").  The order of this list of fields determines the order in which
   * child elements will be rendered, and is optional; if omitted, all getter-style methods with a non-void
   * return type and zero arguments will be treated as field names.
   */
  String[] fields() default {};
  
  UiRendererOptions[] renderers() default {};
}
