package net.wti.gradle.settings;

import net.wti.gradle.api.MinimalProjectView;
import net.wti.gradle.internal.system.InternalProjectCache;
import net.wti.gradle.schema.api.QualifiedModule;
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
import xapi.fu.Lazy;
import xapi.fu.Out1;
import xapi.util.X_Namespace;
import xapi.util.X_String;

import java.io.File;
import java.io.FileInputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

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
    private Lazy<String> group, version;
    private Lazy<Properties> props;

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
        this.props = Lazy.deferred1Unsafe(()->{
            File root = settings.getRootDir();
            File prop = new File(root, "gradle.properties");
            Properties result = new Properties();
            if (prop.exists()) {
                result.load(new FileInputStream(prop));
            }
            return result;
        });
        this.group = Lazy.deferred1(()->{
            String groupId = getGradle().getStartParameter().getProjectProperties().get(X_Namespace.PROPERTY_GROUP_ID);
            if (X_String.isEmpty(groupId)) {
                groupId = (String) props.out1().get(X_Namespace.PROPERTY_GROUP_ID);
                if (X_String.isEmpty(groupId)) {
                    return settings.getRootProject().getName();
                }
            }
            return groupId;
        });

        this.version = Lazy.deferred1(()->{
            String version = getGradle().getStartParameter().getProjectProperties().get(X_Namespace.PROPERTY_VERSION);
            if (X_String.isEmpty(version)) {
                version = (String) props.out1().get(X_Namespace.PROPERTY_VERSION);
                if (X_String.isEmpty(version)) {
                    return QualifiedModule.UNKNOWN_VALUE;
                }
            }
            return version;
        });

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
    public String getGroup() {
        return group.out1();
    }

    @Override
    public String getVersion() {
        return version.out1();
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
