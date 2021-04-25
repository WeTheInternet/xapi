package xapi.annotation.inject;

import xapi.annotation.reflection.MirroredAnnotation;
import xapi.enviro.Enviro;
import xapi.fu.Filter.Filter1;
import xapi.fu.filter.AlwaysFalse;
import xapi.fu.filter.AlwaysTrue;

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

  class None extends AlwaysFalse<Enviro>{}
  class All extends AlwaysTrue<Enviro>{}

  Class<? extends Filter1<Enviro>> isInstance() default All.class;
  int instancePriority() default Integer.MIN_VALUE;

  Class<? extends Filter1<Enviro>> isService() default None.class;
  int servicePriority() default Integer.MIN_VALUE;

  Class<?> value();
}
