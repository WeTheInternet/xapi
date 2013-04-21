package xapi.annotation.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 *
 * @author James X. Nelson (james@wetheinter.net)
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value= {ElementType.FIELD, ElementType.METHOD})
public @interface Persistent {
  /**
   * @return true if a given field is patchable;
   * meaning that it will only be serialized to a server if the value has changed.
   *
   * A patch operation requires an etag request to summarize whether the receiving party
   * is already aware of the current variable's value or not.
   *
   * Setting to false will cause the value to be serialized upon every request.
   */
  boolean patchable() default true;

  PersistenceStrategy strategy() default PersistenceStrategy.Ram;

}
