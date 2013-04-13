package xapi.annotation.model;

import java.lang.annotation.Target;

import xapi.util.api.ValidatesValue;
import xapi.util.validators.ChecksNonNull;
import static java.lang.annotation.ElementType.PARAMETER;

@Target(PARAMETER)
public @interface ParamValidator {
  Class<? extends ValidatesValue<?>>[] validators() default {ChecksNonNull.class};
}
