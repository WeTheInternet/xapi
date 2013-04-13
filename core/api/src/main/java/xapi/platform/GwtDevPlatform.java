package xapi.platform;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import xapi.platform.PlatformSelector.AlwaysTrue;

/**
 * This annotation is used for types that should be injected into gwt dev mode,
 * but not into gwt production mode.  If an injectable type has an @GwtPlatform
 * annotation, but no @GwtDevPlatform, it will emit a warning and use the
 * production mode version instead.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Platform(
  isGwt=true
  ,fallback = {GwtPlatform.class, JrePlatform.class}
  )
public @interface GwtDevPlatform {

  Class<? extends PlatformSelector> runtimeSelector() default AlwaysTrue.class;

}
