package xapi.annotation.reflect;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to tell reflection subsystem to hold onto annotation data.
 * This annotation can be supplied almost anywhere to explicitly include
 * a particular set of annotations in the {@link ReflectionService}.
 * <p>
 * Can be applied to packages, classes, annotation types, parameters,
 * methods, fields and constructors.  The {@link ReflectionService} will do
 * its best to retain manual annotation retention overrides.
 * <p>
 * Primarily used in gwt reflection, but available as well for future
 * iterations of classpath scanner to intelligently discard runtime metadata (anything not annotated).
 * <p>
 * @author "James X. Nelson (james@wetheinter.net)"
 */
@Documented
@Target({PACKAGE, TYPE, ANNOTATION_TYPE, FIELD, METHOD, CONSTRUCTOR, PARAMETER} )
@Retention(RetentionPolicy.RUNTIME)
public @interface KeepAnnotation {

  /**
   * DebugData to include.
   * @return "" (default) = no debug data / production mode
   * "anything" = keep this string, and print extra debug logging.
   * <p>
   * Using debugData.length > 0 will result in as much type data being preserved
   * as possible in log messages, including method default class types.
   */
  String debugData() default "";

  /**
   * Whether or not to defer including reflected constructor
   * into a code split containing all reflection data.
   * <p>
   * Implementations using xapi-gwt-inject won't need to worry as much,
   * the magic-method-injector will ensure code bloat is localized to call site.
   * <p>
   * Running without injection support will put all class data into a single,
   * monolithic, generated factory for the whole app, unless async is used.
   * <p>
   * @return true to force this code into async split.
   * false (default) to load synchronously; no boilerplate.
   * <p>
   * if true,
   * Generator will throw exception if accessed outside of X_Reflect.async()
   */
  boolean loadAsync() default false;

  /**
   * Used to allow package-level or type-level annotations to override children.
   * <p>
   * Good for overriding default settings temporarily; setting debugData and
   * preventOverride on package-info.java will turn on debugging for that package.
   * <p>
   * @return true, to prevent checking for more specific child types.
   * <p>
   * Inheritance Hierarchy Is ->
   * package-info.java -> OuterClass -> OuterClass$InnerClass -> ...
   */
  boolean preventOverride() default false;



}
