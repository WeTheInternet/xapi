package xapi.annotation.model;

import java.lang.annotation.Target;

import xapi.util.api.MatchesValue;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

@Target({METHOD, FIELD})
public @interface QuerierFor {

  String name();
  boolean checksNonNull() default true;
  boolean checksStringNonEmpty() default false;
  String[] checksValidity() default {};
  Class<? extends MatchesValue<?>>[] validators() default {};

}
