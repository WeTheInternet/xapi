package xapi.annotation.process;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use to mark types that should be considered multiplexed.
 *
 * Created for the purpose of attaching multiple session scopes to a given global scope.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 9/24/17.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Multiplexed {
}
