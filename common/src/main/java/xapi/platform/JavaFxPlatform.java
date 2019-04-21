package xapi.platform;

import xapi.platform.PlatformSelector.AlwaysTrue;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Platform(
  fallback = JrePlatform.class
)
public @interface JavaFxPlatform {
  double version() default 8.0;
  Class<? extends PlatformSelector> runtimeSelector() default AlwaysTrue.class;
  Class<? extends Annotation>[] fallback() default JrePlatform.class;
}
