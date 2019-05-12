package xapi.platform;

import xapi.platform.PlatformSelector.AlwaysTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Platform(
  isGwt = true,
  isJava = false,
  // We'll use Gwt3Platform in cases where we want to supercede standard gwt modules,
  // but only when we are explicitly running without the Gwt 2 toolchain.
  // in the future, this relationship should be reversed (Gwt 2 should fallback to Gwt 3)
  fallback = GwtPlatform.class
  )
public @interface Gwt3Platform {

  boolean isGwt() default true;
  boolean isDevMode() default false;
  //Used at generator time to allow selecting runtime based on gwt module properties
  Class<? extends PlatformSelector> runtimeSelector() default AlwaysTrue.class;

}
