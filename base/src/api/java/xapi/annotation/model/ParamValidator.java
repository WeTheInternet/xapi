package xapi.annotation.model;

import xapi.validate.ChecksNonNull;
import xapi.validate.ValidatesValue;

import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;

@Target(PARAMETER)
public @interface ParamValidator {
  Class<? extends ValidatesValue<?>>[] validators() default {ChecksNonNull.class};
}
