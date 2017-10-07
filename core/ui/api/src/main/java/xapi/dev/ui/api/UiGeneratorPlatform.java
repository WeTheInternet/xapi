package xapi.dev.ui.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to put a name / category onto a ui implementation generator.
 *
 * Used to filter out lower priority generators (allows you to replace
 * a given generator on the classpath).
 *
 * To completely ignore a given platform, use system property:
 * -Dxapi.ui.impl.ignored=jfx,web-app,etc
 *
 * Created by James X. Nelson (james @wetheinter.net) on 9/23/17.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface UiGeneratorPlatform {

    String PLATFORM_WEB_APP = "web-app";
    String PLATFORM_WEB_COMPONENT = "component";
    String PLATFORM_JAVA_FX = "jfx";
    String SYSTEM_PROP_IGNORE_PLATFORM = "xapi.ui.impl.ignored";

    String value();

    int priority() default 0;
}
