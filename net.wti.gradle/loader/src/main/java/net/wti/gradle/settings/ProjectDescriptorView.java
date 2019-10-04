package net.wti.gradle.settings;

import net.wti.gradle.internal.api.MinimalProjectView;
import net.wti.gradle.internal.system.InternalProjectCache;
import net.wti.gradle.system.service.GradleService;
import org.gradle.api.initialization.ProjectDescriptor;
import org.gradle.api.initialization.Settings;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.internal.extensibility.DefaultConvention;

import java.io.File;

import static net.wti.gradle.system.service.GradleService.buildOnce;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 30/07/19 @ 1:42 AM.
 */
public class ProjectDescriptorView implements MinimalProjectView {

    private final Settings settings;
    private final ProjectDescriptor descriptor;
    private final ExtensionContainer extensions;

    public ProjectDescriptorView(Settings settings, ProjectDescriptor descriptor) {
        this.settings = settings;
        this.descriptor = descriptor;
        extensions = new DefaultConvention(null);
    }

    public static ProjectDescriptorView fromSettings(Settings settings) {
        final ProjectDescriptor rootProject = settings.getRootProject();
        return buildOnce(settings, rootProject.getPath(),
            s-> new ProjectDescriptorView(settings, rootProject));
    }

    @Override
    public File getProjectDir() {
        return descriptor.getProjectDir();
    }

    @Override
    public Object findProperty(String key) {
        return settings.getStartParameter().getProjectProperties().get(key);
    }

    @Override
    public ExtensionContainer getExtensions() {
        return extensions;
    }
}
