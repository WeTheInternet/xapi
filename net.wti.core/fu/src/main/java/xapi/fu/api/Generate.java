package xapi.fu.api;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation is useful to allow you to add some source code to a member;
 * how you use it is largely up to you; the {@link #lang()} method is
 * useful to specify a particular language (default is "xapi").
 *
 * If you use this annotation for any purpose, you should likely override this value,
 * so that other annotation consumers know they can ignore your dialect.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 10/15/17.
 */
@Documented
// no target because we'll allow this anywhere (it's up to you to decide how to use these)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Generates.class)
public @interface Generate {

    String generatorName() default "";

    Class<?> generatorClass() default Generate.class;// fake null; this annotation clearly is not a generator class

    String lang() default "xapi";

    String[] value() default {};
}
