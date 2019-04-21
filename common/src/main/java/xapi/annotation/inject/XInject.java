package xapi.annotation.inject;

import xapi.annotation.reflect.MirroredAnnotation;
import xapi.enviro.Enviro;
import xapi.util.api.MatchesValue;
import xapi.util.matchers.MatchesAll;
import xapi.util.matchers.MatchesNone;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The successor to the deprecated {@link InstanceDefault}, {@link InstanceOverride} and Singleton counterparts.
 *
 * This injection annotation is applied to any type in order to alert the InjectionService
 * that the type with this annotation is a candidate for injection for the given
 *
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
@Target(value=ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@MirroredAnnotation
public @interface XInject {

  class None extends MatchesNone<Enviro>{}
  class All extends MatchesAll<Enviro>{}

  Class<? extends MatchesValue<Enviro>> isInstance() default All.class;
  int instancePriority() default Integer.MIN_VALUE;

  Class<? extends MatchesValue<Enviro>> isService() default None.class;
  int servicePriority() default Integer.MIN_VALUE;

  Class<?> value();
}
