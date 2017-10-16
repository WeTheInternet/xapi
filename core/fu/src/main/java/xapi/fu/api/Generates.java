package xapi.fu.api;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A list-type annotation for the repeatable {@link Generate} annotation.
 *
 * Normally you should just add multiple @Generate annotations and let the
 * compiler wrap them in @Generates, but you may wish to use this directly,
 * for example, as a member of some other annotation type you declare.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 10/15/17.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Generates {

    Generate[] value() default {};
}
