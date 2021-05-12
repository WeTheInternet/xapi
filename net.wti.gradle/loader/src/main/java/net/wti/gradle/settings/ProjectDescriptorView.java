package net.wti.gradle.settings;

import net.wti.gradle.api.MinimalProjectView;
import net.wti.gradle.schema.api.QualifiedModule;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.initialization.ProjectDescriptor;
import org.gradle.api.initialization.Settings;
import org.gradle.api.internal.CollectionCallbackActionDecorator;
import org.gradle.api.internal.GradleInternal;
import org.gradle.api.internal.SettingsInternal;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.internal.exceptions.DefaultMultiCauseException;
import org.gradle.internal.extensibility.DefaultConvention;
import org.gradle.internal.instantiation.InstantiatorFactory;
import org.gradle.internal.reflect.DirectInstantiator;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.internal.service.ServiceRegistry;
import xapi.fu.Lazy;
import xapi.fu.Out1;
import xapi.constants.X_Namespace;
import xapi.fu.data.ListLike;
import xapi.fu.data.MapLike;
import xapi.fu.data.SetLike;
import xapi.fu.java.X_Jdk;
import xapi.string.X_String;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import static net.wti.gradle.system.service.GradleService.buildOnce;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 30/07/19 @ 1:42 AM.
 */
public class ProjectDescriptorView implements MinimalProjectView {

    public static final String EXT_NAME = "xapiRootView";

    private static final MapLike<Gradle, ProjectDescriptorView> rootViews = X_Jdk.mapWeak();

    private final Settings settings;
    private final Out1<File> projectDir;
    private final ExtensionContainer extensions;
    private final Map<String, ProjectDescriptorView> allProjects;
    private final Gradle gradle;
    private final Instantiator instantiator;
    private final String projectPath;
    private final Lazy<Logger> logger;
    private final Lazy<String> group, version;
    private final Lazy<Properties> props;
    private final SetLike<Action<Settings>> whenSettingsReady;
    private volatile boolean evaluated, settingsReady;

    public ProjectDescriptorView(Settings settings, ProjectDescriptor descriptor) {
        this(descriptor.getPath(), settings, new LinkedHashMap<>(), descriptor::getProjectDir);
    }

    public ProjectDescriptorView(Settings settings, ProjectDescriptor descriptor, Map<String, ProjectDescriptorView> allProjects) {
        this(descriptor.getPath(), settings, allProjects, descriptor::getProjectDir);
    }

    public ProjectDescriptorView(String projectPath, Settings settings, Map<String, ProjectDescriptorView> allProjects, Out1<File> projectDir) {
        this(settings.getGradle(), projectPath, settings, allProjects, projectDir);
    }
    public ProjectDescriptorView(Gradle gradle, String projectPath, Settings settings, Map<String, ProjectDescriptorView> allProjects, Out1<File> projectDir) {
        this.settings = settings;
        this.projectDir = projectDir;
        this.projectPath = projectPath;
        this.gradle = gradle;
        this.allProjects = allProjects;
        this.whenSettingsReady = X_Jdk.setLinked();
        this.logger = Lazy.deferred1(Logging::getLogger, ProjectDescriptorView.class);

        if (settings instanceof SettingsInternal) {
            final ServiceRegistry services = ((SettingsInternal) settings).getGradle().getServices();
            InstantiatorFactory factory = services.get(InstantiatorFactory.class);
            Instantiator inst;
            try {
                inst = factory.inject();
            } catch (NoSuchMethodError e) {
                try {
                    final Method method = factory.getClass().getMethod("inject");
                    final Object result = method.invoke(factory);
                    inst = (Instantiator) result;
                } catch (Exception fatal) {
                    throw new GradleException("Unable to invoke InstantiatorFactory.inject() on " + factory, fatal);
                }
            }
            this.instantiator = inst;
        } else {
            System.out.println(settings + " does not implement SettingsInternal; cannot extract instantiator");
            this.instantiator = DirectInstantiator.INSTANCE;
        }
        DefaultConvention ext;
        try {
            ext = new DefaultConvention(instantiator);
        } catch (NoSuchMethodError e) {
            try {
                ext = (DefaultConvention) DefaultConvention.class.getConstructors()[0].newInstance(instantiator);
            } catch (Exception fatal) {
                throw new DefaultMultiCauseException("Unable to call factory.inject() to get an appropriate instantiator", Arrays.asList(e, fatal));
            }
        }
        extensions = ext;
        this.allProjects.put(projectPath, this);
        evaluated = false;

        whenReady(ready->evaluated=true);
        this.props = Lazy.deferred1Unsafe(()->{
            Properties result = new Properties();
            // recursively add root-most project's gradle.properties, then next eldest ancestor, then current project last.
            final ProjectDescriptor rootDescript = settings.getRootProject();
            ListLike<ProjectDescriptor> stack = X_Jdk.listArray();
            stack.add(rootDescript);

            ProjectDescriptor search = rootDescript;
            final String[] pathSegs = projectPath.split(":");
            int ind = pathSegs.length > 0 && "".equals(pathSegs[0]) ? 1 : 0;
            // instead of this recursion, we could probably get-and-realize our parent view, to cache results and avoid file reloads.
            perproject:
            while (ind < (pathSegs).length) {
                String nextChild = pathSegs[ind];
                ind++;
                for (ProjectDescriptor child : search.getChildren()) {
                    if (nextChild.equals(child.getName())) {
                        stack.add(child);
                        search = child;
                        continue perproject;
                    }
                }
                // TODO: have option to create missing projects...
                throw new IllegalArgumentException("Unable to find declared project " + projectPath +
                        " Failed to find \"" + nextChild + "\" within " + search.getPath());
            }
            // now, iterate through our project stack, and apply gradle.properties up to our project.
            for (ProjectDescriptor proj : stack.clearItems()) {
                File root = proj.getProjectDir();
                File prop = new File(root, "gradle.properties");
                if (prop.exists()) {
                    result.load(new FileInputStream(prop));
                }
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

    public static RootProjectView rootView(Settings settings) {
        final ProjectDescriptor rootProject = settings.getRootProject();
        return buildOnce(settings, EXT_NAME,
            s-> new RootProjectView(settings, rootProject));
    }

    public static RootProjectView rootView(MinimalProjectView view) {
        if (view instanceof RootProjectView) {
            return (RootProjectView) view;
        }
        return rootView(view.getSettings());
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
            RootProjectView.rootView(this).whenAfterSettings(ready-> {
                action.execute(this);
            });
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
    public Logger getLogger() {
        return logger.out1();
    }

    @Override
    public CollectionCallbackActionDecorator getDecorator() {
        return CollectionCallbackActionDecorator.NOOP;
    }

    @Override
    public ProjectDescriptorView findView(String name) {
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
    public RootProjectView getRootProject() {
        return (RootProjectView) findView(":");
    }

    @Override
    public ExtensionContainer getExtensions() {
        return extensions;
    }

    @Override
    public void whenSettingsReady(final Action<Settings> callback) {
        final ProjectDescriptorView root = getRootProject();
        if (root == this) {
            if (root.settingsReady) {
                callback.execute(settings);
            } else {
                root.whenSettingsReady.add(callback);
            }
        } else {
            root.whenSettingsReady(callback);
        }
    }

    public void settingsReady() {
        final ProjectDescriptorView root = getRootProject();
        while (root.whenSettingsReady.isNotEmpty()) {
            for (Action<Settings> callback : root.whenSettingsReady.clearItems()) {
                callback.execute(settings);
            }
        }
        root.settingsReady = true;
    }

    public Settings getSettings() {
        return settings;
    }
}
