package net.wti.gradle.api;

import org.gradle.api.Action;
import org.gradle.api.initialization.Settings;
import org.gradle.api.internal.CollectionCallbackActionDecorator;
import org.gradle.api.internal.GradleInternal;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.internal.build.BuildState;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.util.Path;

import java.io.File;

import static net.wti.gradle.system.tools.GradleCoerce.isEmptyString;
import static net.wti.gradle.system.tools.GradleCoerce.isNotEmptyString;

/**
 * The most basic form of a "universal grab bag of random utilities".
 * <p><p>
 * When settings.gradle is being evaluated, these will be a net.wti.gradle.settings.ProjectDescriptorView
 * <p><p>
 * Once the project build.gradle are being evaluated, these will be a net.wti.gradle.internal.impl.DefaultProjectView
 * <p><p>
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 29/07/19 @ 5:33 AM.
 */
public interface MinimalProjectView extends ExtensionAware {

    File getProjectDir();

    Object findProperty(String key);

    Instantiator getInstantiator();

    CollectionCallbackActionDecorator getDecorator();

    MinimalProjectView findView(String name);

    Gradle getGradle();

    Settings getSettings();

    void whenSettingsReady(Action<Settings> callback);

    void whenReady(Action<? super MinimalProjectView> callback);

    default MinimalProjectView getRootProject() {
        return findView(":");
    }

    default String getBuildName() {
        return extractBuildName(getGradle());
    }

    static String extractBuildName(Gradle gradle) {
        final BuildState owner = ((GradleInternal) gradle).getOwner();
        final Path id = owner.getCurrentPrefixForProjectsInChildBuilds();
        return id.getPath();
    }

    String getGroup();
    String getVersion();

    Logger getLogger();

    static String searchProperty(String key, MinimalProjectView view) {
        String maybe = System.getProperty(key);
        if (isEmptyString(maybe)) {
            if (view != null) {
                Object candidate = view.findProperty(key);
                if (candidate != null) {
                    maybe = String.valueOf(candidate);
                }
            }
        }
        if (isNotEmptyString(maybe)) {
            return maybe;
        }
        // convert to env var; universally forces camelCase, kebob-case and dot.case. to UPPER_SNAKE_CASE
        String envKey = key.replaceAll("([A-Z][a-z]*)", "_$1").replaceAll("[.-]", "_").toUpperCase();
        maybe = System.getenv(envKey);
        if (isNotEmptyString(maybe)) {
            return maybe;
        }
        return null;
    }
}
