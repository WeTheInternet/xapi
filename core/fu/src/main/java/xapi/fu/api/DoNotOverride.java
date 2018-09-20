package xapi.fu.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Use to mark types and methods that should not be overridden by user code (but cannot be made final)
 *
 * Created by James X. Nelson (james @wetheinter.net) on 8/6/17.
 */
// Purposely allowed anywhere, as a hint.
@Retention(RetentionPolicy.SOURCE)
public @interface DoNotOverride {

    String value() default "";
}
