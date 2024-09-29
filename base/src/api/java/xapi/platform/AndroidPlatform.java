package xapi.platform;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import xapi.platform.PlatformSelector.AlwaysTrue;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Platform(
  isMobile=true,
  fallback = JrePlatform.class
)
public @interface AndroidPlatform {
  double version() default 6.0;
  Class<? extends PlatformSelector> runtimeSelector() default AlwaysTrue.class;
  Class<? extends Annotation>[] fallback() default JrePlatform.class;
}
