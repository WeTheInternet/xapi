package net.wti.gradle.internal;

import net.wti.gradle.api.GradleCrossVersionService;
import net.wti.gradle.api.MinimalProjectView;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.initialization.Settings;
import org.gradle.api.internal.CollectionCallbackActionDecorator;
import org.gradle.api.internal.GradleInternal;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.logging.Logger;
import org.gradle.internal.build.BuildState;
import org.gradle.internal.reflect.DirectInstantiator;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.util.Path;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static net.wti.gradle.system.tools.GradleCoerce.isEmptyString;
import static net.wti.gradle.system.tools.GradleCoerce.isNotEmptyString;

/**
 * ProjectViewInternal:
 * <p>
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 08/06/2024 @ 11:42 p.m.
 */
public interface ProjectViewInternal extends MinimalProjectView {

    static Instantiator findInstantiator(MinimalProjectView view) {
        if (view instanceof ProjectViewInternal) {
            return ((ProjectViewInternal) view).getInstantiator();
        }
        return DirectInstantiator.INSTANCE;
    }

    static CollectionCallbackActionDecorator findDecorator(MinimalProjectView view) {
        if (view instanceof ProjectViewInternal) {
            return ((ProjectViewInternal) view).getDecorator();
        }
        return CollectionCallbackActionDecorator.NOOP;
    }

    Instantiator getInstantiator();

    CollectionCallbackActionDecorator getDecorator();

    Gradle getGradle();

    Settings getSettings();

    void whenSettingsReady(Action<Settings> callback);

    void whenReady(Action<? super MinimalProjectView> callback);

    Logger getLogger();

    default String getBuildName() {
        return extractBuildName(getGradle());
    }

    default GradleCrossVersionService getGradleVersionService() {
        return GradleCrossVersionService.getService(getGradle());
    }
    static String extractBuildName(Gradle gradle) {
        final BuildState owner = ((GradleInternal) gradle).getOwner();
        try {
            String path = owner.getBuildIdentifier().getBuildPath();
            return path;
        } catch (NoSuchMethodError e) {
            Object o;
            try {
                Method m = owner.getClass().getMethod("getCurrentPrefixForProjectsInChildBuilds");
                m.setAccessible(true);
                o = m.invoke(owner);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
                throw new GradleException("Can't extract build name!", ex);
            }
            return ((Path)o).getPath();
        }
//        final Path id = owner.getCurrentPrefixForProjectsInChildBuilds();
//        return id.getPath();
    }

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
