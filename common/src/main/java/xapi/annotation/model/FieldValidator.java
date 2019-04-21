package xapi.annotation.model;

import java.lang.annotation.Target;

import xapi.util.api.ValidatesValue;
import xapi.util.validators.ChecksNonNull;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

@Target({METHOD, FIELD})
public @interface FieldValidator {
  Class<? extends ValidatesValue<?>>[] validators() default {ChecksNonNull.class};
}
