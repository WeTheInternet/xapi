package net.wti.gradle.settings;

import org.gradle.BuildAdapter;
import org.gradle.api.initialization.ProjectDescriptor;
import org.gradle.api.initialization.Settings;
import xapi.fu.In1;

/**
 * RootProjectView:
 * <p>
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 24/04/2021 @ 2:19 a.m..
 */
public class RootProjectView extends ProjectDescriptorView {

    private In1<RootProjectView> beforeSettings, afterSettings;

    public RootProjectView(final Settings settings, final ProjectDescriptor descriptor) {
        super(settings, descriptor);
        if (beforeSettings == null) {
            beforeSettings = In1.IGNORED;
        }
        if (afterSettings == null) {
            afterSettings = In1.IGNORED;
        }
        final RootProjectView self = this;
        settings.getGradle().addBuildListener(new BuildAdapter() {
            @Override
            public void settingsEvaluated(final Settings settings) {

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
            }
        });
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
