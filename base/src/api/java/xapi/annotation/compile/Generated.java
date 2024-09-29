package xapi.annotation.compile;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

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
@Repeatable(value = GeneratedList.class)
public @interface Generated {

   /**
    * The value String[] should always start with the fully qualified name of the code generator creating the file.
    * <br/>
    * Strings after the name of the code generator may be used to store hashes
    * that can be checked to prevent re-generation of types that are not stale.
    * <br/>
    * For example:
    * <pre>
    *
    *    @ {@link Generated}( { "com.acme.generator.CodeGen",
    *
    *     "com.foo.Dependency1.java" , "ABC123-Strong-Hash", "1/1/2001-16:20:51:777Z",
    *
    *     "com.bar.Dependency2.css" , "987ZYX-Strong-Hash", "1/1/2001-16:20:57:777Z"
    *    })
    * </pre>
    *
    * This allows a particular generator to know what files it used as inputs,
    * and whether it should bother regenerating, or just reuse the file.
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

  /**
   * Optionally let generators mark what original package of the given type.
   */
  String originalPackage() default "";

  /**
   * Optionally let generators mark what the original enclosed type name.
   *
   * For example, suppose you have an inner type:
   * class Outer { class Inner{} }
   *
   * And you choose a generated filename Outer_Inner__Generated,
   * you would set the enclosed name to "Outer.Inner",
   * so other tools do not have to know about your arbitrary file naming choices.
   */
  String originalEnclosedName() default "";
}

