package xapi.annotation.common;

public @interface Property {

  String name();
  String value() default "";
  
}
