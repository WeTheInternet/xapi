package net.wti.gradle.internal.system;

import net.wti.gradle.system.service.GradleService;
import org.gradle.api.plugins.ExtensionAware;

import java.lang.annotation.*;

/**
 * A marker type for "things we want to glue onto an {@link ExtensionAware} object exactly once,
 * but without exposing the given object to the outside dsl (i.e. a "private" extension.
 *
 * This only works through objects created via {@link InternalProjectCache},
 * like the various {@link GradleService#buildOnce} methods.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 2/23/19 @ 1:43 AM.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface InternalExtension {
}
