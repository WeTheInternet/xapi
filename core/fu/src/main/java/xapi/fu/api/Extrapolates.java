package xapi.fu.api;

import java.lang.annotation.*;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 8/23/18 @ 2:29 AM.
 */
@Documented
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.PACKAGE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Extrapolates {

    Extrapolate[] value() default {};
}
