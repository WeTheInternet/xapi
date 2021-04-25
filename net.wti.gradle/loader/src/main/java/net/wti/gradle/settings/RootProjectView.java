package net.wti.gradle.settings;

import org.gradle.api.initialization.ProjectDescriptor;
import org.gradle.api.initialization.Settings;

/**
 * RootProjectView:
 * <p>
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 24/04/2021 @ 2:19 a.m..
 */
public class RootProjectView extends ProjectDescriptorView {
    public RootProjectView(final Settings settings, final ProjectDescriptor descriptor) {
        super(settings, descriptor);
    }
}
