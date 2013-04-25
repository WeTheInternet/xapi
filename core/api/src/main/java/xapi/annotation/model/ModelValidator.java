package xapi.annotation.model;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import xapi.annotation.reflect.MirroredAnnotation;
import xapi.util.api.ValidatesValue;
import xapi.util.validators.ChecksNonNull;
import static java.lang.annotation.ElementType.TYPE;

/**
 * Define the validator class used to validate this model's fields.
 * 
 * The default validator does not allow null values.
 * 
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
@Documented
@Target(TYPE)
@MirroredAnnotation
@Retention(RetentionPolicy.CLASS)
public @interface ModelValidator {
  Class<? extends ValidatesValue<?>>[] validators() default {ChecksNonNull.class};
}
