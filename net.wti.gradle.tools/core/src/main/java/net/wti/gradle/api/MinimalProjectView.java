package net.wti.gradle.api;

import org.gradle.api.Action;
import org.gradle.api.internal.CollectionCallbackActionDecorator;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.internal.reflect.Instantiator;

import java.io.File;

/**
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

}
