package xapi.fu.api;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;

/**
 * Use to mark a method whose return type or parameter should be automatically overloaded.
 *
 * This can be useful for "return self" fluent apis.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 10/25/18 @ 12:33 AM.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({METHOD, PARAMETER})
public @interface AutoOverload {
}
