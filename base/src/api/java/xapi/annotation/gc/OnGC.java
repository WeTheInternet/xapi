package xapi.annotation.gc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Using the OnGC annotation in collaboration with {@link X_GC} allows objects
 * to cleanly automate the job of cleaning up their children.
 * 
 * Current support only includes calling an ordered list of onDestroy methods.
 * 
 * Use the {@link OnGC#instanceGCMethods()} to define the order in which you want the zero-arg methods called.
 * 
 * 
 * Runtime gc will use some reflection, as efficiently as possible, to allow any object
 * to properly destroy another object without actually knowing anything at all about it's type.
 * 
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface OnGC {

  /**
   * The ordered list of zero-arg gc methods you would like to call,
   * in the order you would like them called.  These must be
   * instance object on the thing you want to destroy.
   * 
   * @return
   */
  String[] instanceGCMethods() default {};
  /**
   * An ordered list of one-arg static methods you want to be executed.
   * 
   * Proper syntax is:
   * com.project.Clazz$InnerStatic#methodToCall

   * @return - A list of one-arg static destruction methods to call.
   * That one arg MUST be java.lang.Object; if you need a typed method,
   * make two.  That's what many jvms do anyway ;) Google: java bridge methods
   */
  String[] staticGCMethods() default {};
  
  /**
   * @return Whether or not to use reflection to destroy instance fields.
   * 
   * Default of true causes all object references to get set to null.
   */
  boolean deleteInstanceFields() default true;
  
  /**
   * @return true to check objects in GC'd instance fields for OnGC,
   * and destroy them as well.  Default is false for runtime perfomance.
   * 
   * Only turn this on if you want to form a destruction chain of objects,
   * and are too lazy to manually call X_GC#destroy() on all your fields.
   * 
   * Note that gwt support for chained destroy is not currently functional.
   */
  boolean chainDeleteFields() default false;
  
}
