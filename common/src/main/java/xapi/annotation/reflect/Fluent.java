/**
 *
 */
package xapi.annotation.reflect;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Use this annotation to specify whether a given method is fluent or not (returns this).
 * <p>
 * This is useful for generators which must try to disambiguate whether you want to return
 * the current refernece, or the this reference.
 *
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
@Documented
@Retention(RUNTIME)
@Target(METHOD)
public @interface Fluent {

  boolean value() default true;

}
