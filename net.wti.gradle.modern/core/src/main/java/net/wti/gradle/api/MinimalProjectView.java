package net.wti.gradle.api;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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
public interface MinimalProjectView extends ExtensionAware, BuildCoordinates {

    File getProjectDir();

    Object findProperty(String key);

    MinimalProjectView findView(String name);

    default MinimalProjectView getRootProject() {
        return findView(":");
    }

    String getBuildName();

    @Override
    String getGroup();
    @Override
    String getVersion();

    Settings getSettings();
}
