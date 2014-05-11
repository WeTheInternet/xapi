package xapi.annotation.compile;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * This is a duplicate of {@link javax.annotation.Generated}, with the exception
 * that this class has a runtime annotation retention level, as we need to
 * have access to it's value from generated class files.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */

@Documented
@Retention(RUNTIME)
@Target({PACKAGE, TYPE, ANNOTATION_TYPE, METHOD, CONSTRUCTOR, FIELD,
        LOCAL_VARIABLE, PARAMETER})
public @interface Generated {
   /**
    * The value element MUST have the name of the code generator.
    * The recommended convention is to use the fully qualified name of the
    * code generator. For example: com.acme.generator.CodeGen.
    * <br/>
    * Strings after the name of the code generator may be used to store hashes
    * that can be checked to prevent re-generation of types that are not stale.
    *
    */
   String[] value();

   /**
    * Date when the source was generated.
    */
   String date() default "";

   /**
    * A place holder for any comments that the code generator may want to
    * include in the generated code.
    */
   String comments() default "";
}