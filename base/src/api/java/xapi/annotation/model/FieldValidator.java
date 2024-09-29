package xapi.annotation.model;

import xapi.validate.ChecksNonNull;
import xapi.validate.ValidatesValue;

import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

@Target({METHOD, FIELD})
public @interface FieldValidator {
  Class<? extends ValidatesValue<?>>[] validators() default {ChecksNonNull.class};
}
