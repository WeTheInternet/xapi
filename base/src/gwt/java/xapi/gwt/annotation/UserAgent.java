package xapi.gwt.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use this annotation on your own marker annotations, to declare that annotation
 * to be a user agent annotation, capable of participating in the generation of selection scripts.
 * <p>
 * Be sure the only return annotation classes annotated with UserAgent in your {@link #fallbacks()}, if any.
 * 
 * @author james@wetheinter.net
 *
 */
@Documented
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface UserAgent {

  String shortName();
  
  String selectorScript();
  
  Class<? extends Annotation>[] fallbacks() default {};
  
}
