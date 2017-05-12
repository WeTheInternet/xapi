package xapi.fu.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to mark any member as ignored,
 * which is a general-purpose signal to
 * compiler tools to ignore this member.
 *
 * In order to provide some context,
 * the value() method returns a String[],
 * by default == { "all" }, which should
 * be acceptable to most generators which
 * actually check for @Ignore.
 *
 * If you wish to create a specialized kind of Ignore
 * which does not trigger unintended tooling,
 * you should either create a more specific annotation,
 * or specify some other tokens, to ignore specific tools,
 * but allow any others to run.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 5/9/17.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
public @interface Ignore {

    String ALL = "all";

    /**
     * Specify any token other than "all"
     * to create a specialized Ignore annotation.
     *
     * Because this is a general annotation,
     * it is up to consumers of Ignore annotations
     * to check if the value == "all" || value == "myToken"
     *
     */
    String[] value() default ALL;
}
