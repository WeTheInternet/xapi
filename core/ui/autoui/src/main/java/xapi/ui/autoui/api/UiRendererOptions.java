package xapi.ui.autoui.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@SuppressWarnings("rawtypes")
@Retention(RetentionPolicy.RUNTIME)
public @interface UiRendererOptions {

  Class<? extends UiRenderer>[] renderers() default {};

  /**
   * @return true if the current set of {@link #renderers()} should be applied to
   * child nodes; when {@link UiRendererOptions} are used on a class, and {@link #isWrapper()}
   * return true, then each renderer is applied to the child members of that class (enclosed types,
   * and enclosed methods, as determined by method return type).
   * <br/>
   * In the future, package-level support will be supplied, to enable easy reusing of templates.
   */
  boolean isWrapper() default false;
  boolean isHead() default false;
  boolean isTail() default false;

  Class<? extends Validator>[] validators() default {};
  
  Class<? extends UiRendererSelector> selector() default AlwaysTrue.class;
  
  String template() default "";
  
  String[] templatekeys() default {
    "$name", "$value",
    "$0", "$1", "$2", "$3", "$4", "$5", "$6", "$7", "$8", "$9"
  };
  
}
