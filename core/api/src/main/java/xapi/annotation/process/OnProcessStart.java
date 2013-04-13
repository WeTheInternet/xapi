package xapi.annotation.process;


import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;

/**
 * A method annotation used to tell the X_Process macro that
 * you wish the method to be called when the process begins.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(METHOD)
public @interface OnProcessStart {

}
