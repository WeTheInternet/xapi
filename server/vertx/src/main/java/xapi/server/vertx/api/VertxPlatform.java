package xapi.server.vertx.api;

import xapi.platform.Platform;
import xapi.platform.PlatformSelector;
import xapi.platform.PlatformSelector.AlwaysTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/15/17.
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Platform(
    isGwt = false,
    isJava = true,
    isServer = true
)
public @interface VertxPlatform {

    Class<? extends PlatformSelector> runtimeSelector() default AlwaysTrue.class;

}
