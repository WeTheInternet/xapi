package xapi.fu.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Use to mark types and methods that should be overridden by user code.
 *
 * For example, in a default method which _can_ provide a suboptimal implementation,
 * like "glue a bunch of iteration and mapping together" versus "actual do the thing directly".
 *
 * Created by James X. Nelson (james @wetheinter.net) on 8/6/17.
 */
@Retention(RetentionPolicy.SOURCE)
public @interface ShouldOverride {

    String value() default "";
}
