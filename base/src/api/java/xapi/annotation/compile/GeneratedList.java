package xapi.annotation.compile;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * The container annotation necessary to allow multiple @Generated annotations to be added to a given type.
 */
@Documented
@Retention(RUNTIME)
@Target(ANNOTATION_TYPE)
@interface GeneratedList {
  Generated[] value();
}
