package xapi.platform;

import xapi.annotation.mirror.MirroredAnnotation;
import xapi.platform.PlatformSelector.AlwaysTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Platform(
  isGwt = true,
  isJava = false
  )
@MirroredAnnotation
public @interface GwtPlatform {

  boolean isGwt() default true;
  boolean isDevMode() default false;
  //Used at generator time to allow selecting runtime based on gwt module properties
  Class<? extends PlatformSelector> runtimeSelector() default AlwaysTrue.class;

  /**
   * Normally, if you access a singleton synchronously before you do asynchronously,
   * the provider will skip using a code-splitting callback,
   * as the injected class would be pulled into the codebase beforehand.
   *
   * Use this flag to force the async provider class, in case you still want to split out
   * other dependencies in your callback (the service won't make it into the splitpoint,
   * but everything you do with it will.)
   *
   * @return - True to always route calls through the async provider for code splitting.
   */
  boolean forceAsync() default false;

  //TODO allow user-agent support by tying into the rebind permutation oracle
  //(will require replace-with rules).
  //Another option might be a feature-sniffer class,
  //which will cause gwt to compile in all possible rebind results,
  //and select the desired type with runtime feature detection.

  //Feature sniffing is NOT wise for user-agent based switching,
  //but could prove useful for detecting un/prefixed native browser methods.

}
