package xapi.annotation.model;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import xapi.annotation.mirror.MirroredAnnotation;

/**
 * An annotation to specify that an abstract method is a getter for a model field.
 *
 * This must be placed either on a no-arg method,
 * or a single-arg method with a parameter type assignable to return type (value-if-null)
 *
 * If this annotation is missing from a zero-arg method structured with a "getter-like-name",
 * and that method does not have any other @*For model field annotations,
 * then the method will be treated as an implied @GetterFor.
 *
 * In other words, you don't need to annotate either of these methods:
 * String getField();
 * String field();
 *
 * But if the field's name will be "field", then you would have to annotate:
 * @GetterFor("field")
 * String getTheField();
 * And in order to prevent ambiguity, you also must annotate getters with default values:
 * @GetterFor("field")
 * String getField(String dflt);
 *
 * This restriction prevents ambiguity problems when generating a fluent setter api;
 * a fluent setter could look like a getter with a default value,
 * so we suggest both to be properly annotated,
 * and fallback to checking for methodName.startsWith("get") || .startsWith("set").
 *
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@MirroredAnnotation
public @interface GetterFor {

  String value() default "";

}
