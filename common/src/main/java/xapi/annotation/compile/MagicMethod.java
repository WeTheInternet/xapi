/**
 *
 */
package xapi.annotation.compile;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;


/**
 * This is a marker type for method that are "magic", aka, replaced by the
 * Gwt compiler.  Use the {@link #doNotVisit()} method to tell the compiler
 * that the body of the method should not be visited.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
@Documented
@Retention(CLASS)
@Target(METHOD)
public @interface MagicMethod {

  String documentation() default "";

  boolean doNotVisit() default true;

  Reference generator() default @Reference;
}
