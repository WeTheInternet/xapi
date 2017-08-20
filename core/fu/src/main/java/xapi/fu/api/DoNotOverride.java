package xapi.fu.api;

/**
 * Use to mark types and methods that should not be overridden by user code (but cannot be made final)
 *
 * Created by James X. Nelson (james @wetheinter.net) on 8/6/17.
 */
public @interface DoNotOverride {

    String value() default "";
}
