package xapi.fu.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Use to mark types and methods that should not be overridden by user code (but cannot be made final).
 *
 * Typically, this is used when you have a pair of methods in an interface/class,
 * like isResolved()+isUnresolved(), where you can default one of them to call the other.
 *
 * If you do override such a method (for example, to provide runtime-optimized code),
 * try to mark it final when you do; when we add warnings for this, we will ignore
 * anything that is either marked final, or also annotated with @DoNotOverride
 * (i.e. developer saying "I know what I've done when I disobeyed recommendations").
 *
 *
 * Created by James X. Nelson (james @wetheinter.net) on 8/6/17.
 */
// Purposely allowed anywhere, as a hint.
@Retention(RetentionPolicy.SOURCE)
public @interface DoNotOverride {

    String value() default "";
}
