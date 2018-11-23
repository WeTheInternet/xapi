package xapi.gradle.plugin;

import org.gradle.api.*;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.internal.DefaultDomainObjectSet;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.internal.reflect.Instantiator;
import xapi.dev.source.DomBuffer;
import xapi.dev.source.PrintBuffer;
import xapi.fu.Do;
import xapi.fu.In1Out1;
import xapi.fu.Lazy;
import xapi.fu.Out1;
import xapi.fu.itr.Chain;
import xapi.fu.itr.ChainBuilder;
import xapi.gradle.api.AllSources;
import xapi.gradle.api.AllJars;
import xapi.gradle.api.ArchiveType;
import xapi.gradle.config.PublishConfig;
import xapi.gradle.task.XapiInit;
import xapi.gradle.task.XapiManifest;

import java.io.File;
import java.io.IOException;

/**
 * This is the `xapi { }` project extension object which is configured in your build script.
 *
 * This object outlines the various levers and knobs that you can configure when using the xapi gradle plugin.
 *
 * Some of these additional features will require platform-specific plugins to be installed,
 * and this will be done automatically unless you specify autoPlugin = false
 *
 * There will be subtypes of this config that are installed specially;
 * for now, only XapiExtensionDist will be used, which is applied through a specific plugin.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 11/4/18 @ 1:51 AM.
 */
public class XapiExtension {

    public static final String EXT_NAME = "xapi";
    private final Property<String> prefix;
    private final Property<Boolean> autoPlugin;
    private final Property<PublishConfig> publish;
    private final Property<AllSources> sources;
    private final Property<AllJars> jars;
    private final DirectoryProperty outputMeta;
    private final TaskProvider<XapiInit> initTask;
    private final DomainObjectSet<In1Out1<TaskProvider<XapiInit>, TaskProvider<?>>> beforeInit, afterInit;
    private final Out1<XapiInit> init;
    private final ChainBuilder<Do> onInit, onPrepare, onFinish;
    private final Logger logger;

    public XapiExtension(Project project, Instantiator instantiator) {
        logger = project.getLogger();
        autoPlugin = lockable(project, Boolean.class);
        prefix = lockable(project, String.class);
        publish = lockable(project, PublishConfig.class);
        sources = lockable(project, AllSources.class);
        jars = lockable(project, AllJars.class);

        Lazy<AllSources> lazySource = Lazy.deferred1(this::prepareSources, instantiator);
        sources.set(project.provider(lazySource::out1));
        Lazy<AllJars> lazyJars = Lazy.deferred1(this::prepareJars, project);
        jars.set(project.provider(lazyJars::out1));

        outputMeta = project.getObjects().directoryProperty();
        outputMeta.set(
            project.getLayout().getBuildDirectory().dir("xapi-paths")
        );

        onInit = Chain.startChain();
        onPrepare = Chain.startChain();
        onFinish = Chain.startChain();
        // Add a sentinel value; if the list is empty, we will immediately execute the callback.
        onInit.add(Do.NOTHING);
        onPrepare.add(Do.NOTHING);
        onFinish.add(Do.NOTHING);

        configureDefaults(project);

        beforeInit = new DefaultDomainObjectSet<In1Out1<TaskProvider<XapiInit>, TaskProvider<?>>>(Class.class.cast(In1Out1.class));
        afterInit = new DefaultDomainObjectSet<In1Out1<TaskProvider<XapiInit>, TaskProvider<?>>>(Class.class.cast(In1Out1.class));

        // Register a xapiInit task.  Do not realize it yet, as it's quite expensive to bind.
        // Implicitly depends on parent xapiInit tasks.
        initTask = project.getTasks().register(XapiInit.INIT_TASK_NAME, XapiInit.class, init->{
            if (!project.getPlugins().hasPlugin(XapiRootPlugin.class)) {
                if (project.getParent() == null) {
                    if (project != project.getRootProject()) {
                        init.getLogger().warn("No path to root module from {} had XapiRootPlugin", project);
                    }
                } else {
                    project.getParent().getPlugins().withType(XapiBasePlugin.class).all(
                        plugin ->
                            init.dependsOn(project.getParent().getTasks().named(XapiInit.INIT_TASK_NAME))
                    );
                }
                init.configure(this);
            }

        });
        // use the .all() method of our domain object collections to get "called back for every element now,
        // plus each new element as it is added in the future" semantics.
        beforeInit.all(cb -> {
            final TaskProvider<?> task = cb.io(initTask);
            if (task != null) {
                initTask.configure(init->init.dependsOn(task));
            }
        });
        project.getLogger().info("xapiInit configuring; {} w/ state {}", project, project.getState());

        project.getTasks().named(
            JavaPlugin.COMPILE_JAVA_TASK_NAME,
            // We want to run init before we even start compiling the sources.
            // This ensures that any javac compiler plugins that want to peak around for some
            // generated resource files will have an opportunity to do so.
            t -> t.dependsOn(initTask)
        );
        project.getTasks().named(JavaPlugin.PROCESS_RESOURCES_TASK_NAME,
            t -> {
                // Whenever resources are requested to be processed, make sure we init first.
                t.dependsOn(initTask);
            }
        );

        afterInit.all(cb -> {
            final TaskProvider<?> task = cb.io(initTask);
            if (task != null) {
                // after xapiInit, immediately start the new task.
                initTask.configure(init -> {
                    init.finalizedBy(task);
                    // eagerly realize the postInit task, so it can dependOn this afterInit task
                    init.getPostInit().get().dependsOn(task);
                });
            }
        });

        init = Lazy.deferred1( ()-> {
            final Task task = project.getTasks().getByName(XapiInit.INIT_TASK_NAME);
            if (!"true".equals(project.findProperty("no.warn.xapi.init"))) {
                project.getLogger().warn("Accessing xapi.init task directly; forcing early resolution." +
                    "  See XapiInit javadoc for details. Set -Pno.warn.xapi.init=true to suppress this warning.");
            }
            return (XapiInit)task;
        });
    }

    public static XapiExtension from(Project project) {
        final Object ext = project.getExtensions().findByName(EXT_NAME);
        if (ext == null) {
            throw new GradleException("No " + EXT_NAME + " found in " + project.getPath() + " ; you need to apply XapiBasePlugin sooner!");
        }
        return (XapiExtension) ext;
    }

    protected void defer(Do task) {
        if (onInit.isEmpty()) {
            if (onPrepare.isEmpty()) {
                onFinish(task);
            } else {
                onPrepare(task);
            }
        } else {
            onInit(task);
        }
    }

    protected AllJars prepareJars(Project project) {
        return new AllJars(project);
    }
    protected AllSources prepareSources(Instantiator instantiator) {
        return new AllSources(instantiator);
    }

    protected <T> FreezingLockProperty<T> lockable(Project project, Class<T> cls) {
        return new FreezingLockProperty<>(project, cls);
    }

    protected void configureDefaults(Project project) {
        if (project != project.getRootProject()) {
            Project p;
            XapiExtension config = null;

            p = project.getParent();
            while (p != null && config == null) {
                config = p.getExtensions().findByType(XapiExtension.class);
                p = p.getParent();
            }
            if (config == null) {
                project.getLogger().quiet("No xapi plugin found between " + project.getPath() + " and root project; auto-installing XapiBasePlugin.\n" +
                    "You should either `plugins { id 'xapi-root' }` or `apply plugin: 'xapi-root'` (or any xapi-suffix plugin) in your root build script.\n" +
                    "If a parent module does have such a plugin defined, be sure to set `evaluationDependsOn ':parent-path'` in the child buildscript.");
                project.getRootProject().getPlugins().apply(XapiRootPlugin.class);
                config = project.getRootProject().getExtensions().findByType(XapiExtension.class);
            }
            inherit(config);
            return;
        }

        // Here is where we actually extract details from project/System
        autoPlugin.set(true);

        publish.set(new PublishConfig());

        // look for xapiPrefix String property.  System prop wins
        String pre = System.getProperty("xapiPrefix");
        if (pre == null) {
            // next, check the project for properties.  This should be the most common way to configure.
            pre = (String) project.findProperty("xapiPrefix");
        }
        if (pre == null) {
            // next, check with the environment
            pre = System.getenv("xapiPrefix");
        }
        if (pre == null) {
            pre = "";
        }
        this.prefix.set(pre);
    }

    protected void inherit(XapiExtension config) {
        if (config == this) {
            throw new IllegalStateException("Cannot inherit self!");
        }
        autoPlugin.set(config.autoPlugin);
        prefix.set(config.prefix);
        publish.set(config.publish);

    }

    public Property<Boolean> getAutoPlugin() {
        return autoPlugin;
    }

    public void setAutoPlugin(Provider<Boolean> autoPlugin) {
        this.autoPlugin.set(autoPlugin);
    }

    public void setAutoPlugin(Boolean auto) {
        this.autoPlugin.set(auto);
    }

    public void setPrefix(String prefix) {
        this.prefix.set(prefix);
    }
    /**
     * Called only when autoPlugin was true for our project, after all projects are evaluated.
     *
     * This will add any autoPlugins that are needed based on other configuration,
     * which will then themselves have a chance to make more modifications before configuration is finished.
     *
     * @param project
     */
    protected void addPlugins(Project project) {

    }

    /**
     * Called after the given xapi-enabled project has been evaluated.
     *
     * Any auto-plugins that would be installed will have already been installed by now.
     *
     * @param project
     */
    protected void initialize(Project project) {
        autoPlugin.finalizeValue();
        if (autoPlugin.get()) {
            addPlugins(project);
        }
        onInit.removeAll(Do.INVOKE);

    }

    /**
     * Called after all projects have been evaluated,
     * but before any XapiExtension has been {@link #finish(Project)}ed.
     *
     * {@link XapiExtension#initialize(Project)} will have been called on all XapiConfigs,
     * and if autoPlugin is enabled, those plugins will all have been added.
     *
     * @param project
     */
    protected void prepare(Project project) {
        publish.finalizeValue();
        PublishConfig config = publish.get();

        if (!config.isHidden()) {
            jars.get().getSources().configure(sourceJar ->
                sourceJar.onlyIf(ignored->config.isSources())
            );
        }
        onPrepare.removeAll(Do.INVOKE);
    }

    /**
     * Called after all projects have fully evaluated, and all XapiExtension have been {@link XapiExtension#prepare(Project)}ed.
     *
     * All plugins that are going to contribute to the final task execution graph
     * have been installed and given a chance to run basic initialization.
     *
     * Anything that needs to touch the task graph has to be finished in this method.
     *
     * @param project
     */
    protected void finish(Project project) {
        onFinish.removeAll(Do.INVOKE);
    }

    public Property<PublishConfig> getPublish() {
        return publish;
    }

    public void publish(Action<? super PublishConfig> configure) {
        // TODO: consider holding onto the action and waiting to invoke it;
        configure.execute(publish.get());
    }

    public Property<AllJars> getJars() {
        return jars;
    }

    public Property<AllSources> getSources() {
        return sources;
    }

    public TaskProvider<XapiInit> getInitProvider() {
        return initTask;
    }

    public XapiInit getInit() {
        return init.out1();
    }

    public void init(Action<? super XapiInit> onInit) {
        initTask.configure(onInit);
    }

    public void beforeInit(In1Out1<TaskProvider<XapiInit>, TaskProvider<?>> factory) {
        beforeInit.add(factory);
    }

    public void afterInit(In1Out1<TaskProvider<XapiInit>, TaskProvider<?>> factory) {
        afterInit.add(factory);
    }

    public void onInit(Do task) {
        if (onInit.isEmpty()) {
            task.done();
        } else {
            onInit.add(task);
        }
    }

    public void onPrepare(Do task) {
        if (onPrepare.isEmpty()) {
            task.done();
        } else {
            onPrepare.add(task);
        }
    }

    public void onFinish(Do task) {
        if (onFinish.isEmpty()) {
            task.done();
        } else {
            onFinish.add(task);
        }
    }

    public String exportSettings(XapiManifest manifest, ArchiveType type) {
        DomBuffer out = new DomBuffer("xapi", false);
        out.allowAbbreviation(true);
        manifest.printArchive(out, manifest, type);
        return out.toSource();
    }

    public DirectoryProperty getOutputMeta() {
        return outputMeta;
    }

    public Directory outputMeta() {
        outputMeta.finalizeValue();
        return outputMeta.get();
    }

    public Logger getLogger() {
        return logger;
    }
}
