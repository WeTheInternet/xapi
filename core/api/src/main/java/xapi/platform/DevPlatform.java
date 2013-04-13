package xapi.platform;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import xapi.platform.PlatformSelector.AlwaysTrue;

/**
 * This annotation is used for types that should be injected into build tools,
 * but not into production environments.  The dev platform is where codegen,
 * maven plugins and transpiler code is placed.  It can also be used for
 * unit testing, provided you set the xapi.platform property to
 * xapi.platform.DevPlatform; you may wish to create your own
 * test platform(s), though it's easier to simply use DevPlatform, and
 * just create different platform selectors to create testing environments.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Platform(
  isDebug=true,
  fallback = JrePlatform.class
)
public @interface DevPlatform {

  Class<? extends PlatformSelector> runtimeSelector() default AlwaysTrue.class;
  Class<? extends Annotation>[] fallback() default JrePlatform.class;
}

