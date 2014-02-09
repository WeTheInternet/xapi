package xapi.ui.autoui.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface UiOptions {

  Class<?>[] imports() default {};
  
  UiRendererOptions[] renderers() default {};
}
