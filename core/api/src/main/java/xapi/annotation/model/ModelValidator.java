package xapi.annotation.model;

import java.lang.annotation.Target;

import xapi.util.api.ValidatesValue;
import xapi.util.validators.ChecksNonNull;
import static java.lang.annotation.ElementType.TYPE;

@Target(TYPE)
public @interface ModelValidator {
  Class<? extends ValidatesValue<?>>[] validators() default {ChecksNonNull.class};
}
