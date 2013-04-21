package xapi.annotation.reflect;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Using this annotation on any other annotation will tell our 
 * annotation processor to generate an annotation mirror class.
 * 
 * Whew.  
 * 
 * In short, we generate classes to represent annotations,
 * so we can easily get at annotation values loaded outside a ClassLoader.
 * 
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
@Documented
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface MirroredAnnotation {

  /**
   * @return whether or not to generate methods that attempt to expose
   * reflective instances of the actual annotation.  
   * 
   * The default is false, to avoid forcing annotation mirrors to
   * include code that may be restricted by SecurityManager.
   */
  boolean generateReflectionFactory() default false;
  
  /**
   * @return whether or not to generate factories for annotation processors.
   * 
   * These factories are generated outside the annotation mirror,
   * to avoid dependency on javax.lang.model.
   * 
   */
  boolean generateJavaxLangModelFactory() default true; 
  
  /**
   * @return whether or not to generate factories for xapi-dev-bytecode.
   * 
   * These factories will be generated in client-accessible code,
   * so the default value is false.
   * 
   * This feature will be more useful when gwt can choke down bytecode.
   */
  boolean generateXapiBytecodeFactory() default false; 
}
