package xapi.annotation.compile;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 1/9/16.
 */
public @interface Reference {

  class UseTypeName {}

  String[] typeName() default {};
  Class type() default UseTypeName.class;
}
