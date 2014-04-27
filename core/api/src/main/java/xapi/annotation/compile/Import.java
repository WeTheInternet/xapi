package xapi.annotation.compile;

public @interface Import {

  Class<?> value();
  String staticImport() default "";
}
