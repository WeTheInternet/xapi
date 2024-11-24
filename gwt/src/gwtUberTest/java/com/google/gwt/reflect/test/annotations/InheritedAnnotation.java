/**
 *
 */
package com.google.gwt.reflect.test.annotations;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * An inherited annotation, used to test that our compiler magic properly obeys requests
 * to include or disclude inherited annotations.
 *
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
@Inherited
@Retention(RUNTIME)
public @interface InheritedAnnotation {
  String blah() default "";
}
