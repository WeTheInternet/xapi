package xapi.annotation.compile;

public @interface Property {

  String name();
  String value() default "";
  
}
