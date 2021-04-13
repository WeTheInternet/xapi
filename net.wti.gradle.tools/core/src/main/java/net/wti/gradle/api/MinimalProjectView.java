package net.wti.gradle.api;

import org.gradle.api.Action;
import org.gradle.api.artifacts.component.BuildIdentifier;
import org.gradle.api.internal.CollectionCallbackActionDecorator;
import org.gradle.api.internal.GradleInternal;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.composite.internal.DefaultIncludedBuild;
import org.gradle.internal.build.BuildState;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.util.Path;

import java.io.File;

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

    void whenReady(Action<? super MinimalProjectView> callback);

    default void whenSettingsReady(Action<? super MinimalProjectView> callback) {
        whenReady(callback);
    }

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

}
