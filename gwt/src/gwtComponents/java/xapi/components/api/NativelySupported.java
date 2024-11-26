package xapi.components.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This is a marker annotation to use on web component methods that are supplied
 * by the underlying DOM element. This is used to tell the generator to skip the
 * method in question; useful in suppressing warning messages about methods that
 * cannot be implemented by the generator.
 *
 * @author James X Nelson (james@wetheinter.net)
 *
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface NativelySupported {
}
