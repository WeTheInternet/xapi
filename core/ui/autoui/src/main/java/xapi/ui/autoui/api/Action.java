package xapi.ui.autoui.api;

public @interface Action {

  Class<?>[] scope() default {};
  String name();
  
}
