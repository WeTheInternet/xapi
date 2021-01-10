package net.wti.gradle.settings;

import net.wti.gradle.api.MinimalProjectView;
import net.wti.gradle.internal.system.InternalProjectCache;
import net.wti.gradle.system.service.GradleService;
import org.gradle.api.Action;
import org.gradle.api.initialization.ProjectDescriptor;
import org.gradle.api.initialization.Settings;
import org.gradle.api.internal.CollectionCallbackActionDecorator;
import org.gradle.api.internal.SettingsInternal;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.internal.extensibility.DefaultConvention;
import org.gradle.internal.reflect.DirectInstantiator;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.internal.service.ServiceRegistry;
import xapi.fu.Out1;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import static net.wti.gradle.system.service.GradleService.buildOnce;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 30/07/19 @ 1:42 AM.
 */
public class ProjectDescriptorView implements MinimalProjectView {

    private final Settings settings;
    private final Out1<File> projectDir;
    private final ExtensionContainer extensions;
    private final Map<String, ProjectDescriptorView> allProjects;
    private final Gradle gradle;
    private final Instantiator instantiator;
    private final String projectPath;
    private boolean evaluated;

    public ProjectDescriptorView(Settings settings, ProjectDescriptor descriptor) {
        this(descriptor.getPath(), settings, new LinkedHashMap<>(), descriptor::getProjectDir);
    }

    public ProjectDescriptorView(Settings settings, ProjectDescriptor descriptor, Map<String, ProjectDescriptorView> allProjects) {
        this(descriptor.getPath(), settings, allProjects, descriptor::getProjectDir);
    }

    public ProjectDescriptorView(String projectPath, Settings settings, Map<String, ProjectDescriptorView> allProjects, Out1<File> projectDir) {
        this.settings = settings;
        this.projectDir = projectDir;
        this.projectPath = projectPath;
        this.gradle = settings.getGradle();
        this.allProjects = allProjects;
        if (settings instanceof SettingsInternal) {
            final ServiceRegistry services = ((SettingsInternal) settings).getGradle().getServices();
            this.instantiator = services.get(Instantiator.class);
        } else {
            System.out.println(settings + " does not implement SettingsInternal; cannot extract instantiator");
            this.instantiator = DirectInstantiator.INSTANCE;
        }
        extensions = new DefaultConvention(this.instantiator);
        this.allProjects.put(projectPath, this);
        evaluated = false;
        whenReady(ready->evaluated=true);

    }

    public static ProjectDescriptorView fromSettings(Settings settings) {
        final ProjectDescriptor rootProject = settings.getRootProject();
        return buildOnce(settings, rootProject.getPath(),
            s-> new ProjectDescriptorView(settings, rootProject));
    }

    @Override
    public File getProjectDir() {
        return projectDir.out1();
    }

    @Override
    public Object findProperty(String key) {
        return settings.getStartParameter().getProjectProperties().get(key);
    }

    @Override
    public Instantiator getInstantiator() {
        return instantiator;
    }

    @Override
    public Gradle getGradle() {
        return gradle;
    }

    @Override
    public void whenReady(Action<? super MinimalProjectView> action) {
        if (evaluated) {
            action.execute(this);
        } else {
            settings.getGradle().settingsEvaluated(ready->action.execute(this));
        }
    }

    @Override
    public CollectionCallbackActionDecorator getDecorator() {
        return CollectionCallbackActionDecorator.NOOP;
    }

    @Override
    public MinimalProjectView findView(String name) {
        String path = name.startsWith(":") ? name : ":" + name;
        if (name.equals(projectPath)) {
            return this;
        }
        final ProjectDescriptorView result = allProjects.get(path);
        if (result == null) {
            ProjectDescriptor subproj = settings.findProject(path);
            if (subproj == null) {
                settings.include(path);
                subproj = settings.findProject(path);
                if (subproj == null) {
                    throw new IllegalArgumentException("No such project " + name + " in project list " + allProjects.keySet());
                }
            }
            return new ProjectDescriptorView(settings, subproj, allProjects);
        }
        return result;
    }

    @Override
    public ExtensionContainer getExtensions() {
        return extensions;
    }
}
