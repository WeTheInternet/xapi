package xapi.annotation.reflect;

import java.lang.annotation.Annotation;

/**
 * The common structure of all @Keep____ Reflection annotations.
 * Currently {@link KeepConstructor}, {@link KeepMethod}
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public interface ReflectionAnnotation extends Annotation{

  /**
   * DebugData to include.
   * @return "" (default) = no debug data / production mode
   * "anything" = keep this string, and print extra debug logging.
   */
  String debugData();

  /**
   * Whether or not to defer including reflected constructor
   * into a code split containing all reflection data.
   *
   * Implementations using xapi-gwt-inject won't need to worry as much,
   * the magic-method-injector will ensure code bloat is localized to call site
   * and trimmed
   *
   * @return true to force this code into async split.
   * Generator will throw exception if accessed outside of X_Reflect.async()
   */
  boolean loadAsync();

  /**
   * Used to allow package-level or type-level annotations to override children.
   *
   * Good for overriding default settings temporarily; setting debugData and
   * preventOverride on package-info.java will turn on debugging for that package.
   *
   * @return true, to prevent checking for more specific child types.
   *
   * Inheritance Hierarchy Is ->
   * package-info.java -> OuterClass -> OuterClass$InnerClass -> ...
   */
  boolean preventOverride();

}
