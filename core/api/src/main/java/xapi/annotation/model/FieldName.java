package xapi.annotation.model;

public @interface FieldName {
  /**
   * Compact name, appropriate for obfuscated builds.
   *
   * @return - Field name to use in production mode ${xapi.prod} = true.
   */
  String name();

  /**
   * If debug name is specified, it will only be used when xapi.prod != true.
   *
   * @return human-friendly name, or "" to use obfuscated name.
   */
  String debugName() default "";
}
