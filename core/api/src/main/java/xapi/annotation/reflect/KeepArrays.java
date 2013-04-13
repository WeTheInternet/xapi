package xapi.annotation.reflect;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Instructs our reflection service to generator a provider for array types.
 * 
 * This allows systems which cannot access java.lang.reflect.Array,
 * such as GWT, to still support creating arrays from a known component class type.
 * 
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface KeepArrays {

  int arrayDepth();
  Class<?>[] alsoKeep() default {};
  
}
