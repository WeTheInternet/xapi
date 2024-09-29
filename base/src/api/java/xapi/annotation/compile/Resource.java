package xapi.annotation.compile;

import xapi.annotation.mirror.MirroredAnnotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A compile-time resource; default behavior is to treat {@link #value()} as a
 * resource location, and use the current thread classloader to get that resource.
 * <p>
 * The {@link #type()} method is used to specify what kind of resource is being included;
 * allowing a compiler service to know how it should be interpretting the annotation value.
 * <p>
 * This annotation is only meant to be used as values for other annotations,
 * and its implementation is dependent on the compiler class using it.
 * <p>
 * The use of the {@link #qualifier()} method is strictly implementation dependent;
 * it is used when the default {@link ResourceType}s are not enough to fully describe
 * how to treat the given dependency.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
@Documented
@Target({})
@Retention(RetentionPolicy.RUNTIME)
@MirroredAnnotation
public @interface Resource {

  enum ResourceType {
    CLASSPATH_RESOURCE, LITERAL_VALUE, ABSOLUTE_FILE, CLASS_NAME, PACKAGE_NAME, ARTIFACT_ID
  }

  String value();
  ResourceType type() default ResourceType.CLASSPATH_RESOURCE;
  String qualifier() default "";

  /**
   * @return false if it's ok that a requested resource is not found;
   * default is true.
   */
  boolean required() default true;

}
