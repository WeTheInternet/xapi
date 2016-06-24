package xapi.annotation.reflect;

import xapi.annotation.compile.Reference;

/**
 * Used to describe a method reference.
 *
 * Intentionally used anywhere,
 * as you may want to use this annotation as a bean in other annotations.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 6/19/16.
 */
public @interface MethodRef {

  Reference cls();
  String name();
  Reference[] params() default {};

}
