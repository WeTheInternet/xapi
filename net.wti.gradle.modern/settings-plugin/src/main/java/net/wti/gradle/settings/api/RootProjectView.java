package net.wti.gradle.settings.api;

import net.wti.gradle.api.MinimalProjectView;
import net.wti.gradle.internal.ProjectViewInternal;
import org.gradle.BuildAdapter;
import org.gradle.api.initialization.ProjectDescriptor;
import org.gradle.api.initialization.Settings;
import org.gradle.api.internal.GradleInternal;
import org.gradle.api.invocation.Gradle;
import xapi.fu.In1;

import java.lang.reflect.InvocationTargetException;

import static net.wti.gradle.tools.InternalGradleCache.buildOnce;

/**
 * RootProjectView:
 * <p>
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 24/04/2021 @ 2:19 a.m..
 */
public class RootProjectView extends ProjectDescriptorView {

    public static final String EXT_NAME = "_xapiRootProject";
    private static final String EXTENSION_NAME = "_xapiRoot";
    private In1<RootProjectView> beforeSettings, afterSettings;

    public RootProjectView(final Settings settings, final ProjectDescriptor descriptor) {
        super(settings, descriptor);
        settings.getExtensions().add(EXTENSION_NAME, this);
        if (beforeSettings == null) {
            beforeSettings = In1.IGNORED;
        }
        if (afterSettings == null) {
            afterSettings = In1.IGNORED;
        }
        final RootProjectView self = this;
        settings.getGradle().settingsEvaluated(ready -> {


                // before we call into universally accessible whenSettingsReady, clean out beforeSettings.
                In1<RootProjectView> copyBefore, copyAfter;
                while (beforeSettings != In1.IGNORED) {
                    synchronized (self) {
                        copyBefore = beforeSettings;
                        beforeSettings = In1.IGNORED;
                    }
                    copyBefore.in(self);
                }

                // call anything waiting on whenSettingsReady
                settingsReady();

                // drain sets of beforeSettings,afterSettings until both are empty.
                // jobs are processed in batches.
                while (beforeSettings != In1.IGNORED && afterSettings != In1.IGNORED) {
                    synchronized (self) {
                        copyBefore = beforeSettings;
                        copyAfter = afterSettings;
                        beforeSettings = afterSettings = In1.IGNORED;
                    }
                    copyBefore.in(self);
                    copyAfter.in(self);
                }
        });
    }

    public static RootProjectView rootView(Settings settings) {
        final ProjectDescriptor rootProject = settings.getRootProject();
        return buildOnce(settings, EXT_NAME,
                s-> new RootProjectView(settings, rootProject));
    }

    public static RootProjectView rootView(MinimalProjectView view) {
        if (view instanceof RootProjectView) {
            return (RootProjectView) view;
        }
        if (view instanceof ProjectViewInternal) {
            return rootView(((ProjectViewInternal) view).getSettings());
        }
        throw new IllegalArgumentException("Cannot find a RootProjectView from " + view.getClass() + ": " + view);
    }

    public static RootProjectView rootView(final Gradle gradle) {
        Settings settings;
        try {
            settings = ((GradleInternal) gradle).getSettings();
        } catch (Exception original) {
            try {
                settings = (Settings)gradle.getClass().getMethod("getSettings").invoke(gradle);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException("Can't get Settings from Gradle " + gradle.getClass() + " : " + gradle, e.getCause() == null ? e : e.getCause());
            }
        }
        return rootView(settings);
    }

    @Override
    public RootProjectView getRootProject() {
        return this;
    }

    public synchronized RootProjectView whenBeforeSettings(In1<RootProjectView> cb) {
        if (beforeSettings == null) {
            beforeSettings = cb;
        } else if (cb != In1.IGNORED && cb != null) {
            beforeSettings = beforeSettings.useAfterMe(cb);
        }
        return this;
    }

    public synchronized RootProjectView whenAfterSettings(In1<RootProjectView> cb) {
        if (afterSettings == null) {
            afterSettings = cb;
        } else if (cb != In1.IGNORED && cb != null) {
            afterSettings = afterSettings.useAfterMe(cb);
        }
        return this;
    }
}
