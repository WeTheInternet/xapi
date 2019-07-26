package xapi.gradle.plugin;

import net.wti.gradle.PublishXapi;
import net.wti.gradle.system.spi.GradleServiceFinder;
import org.gradle.api.*;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.internal.DefaultDomainObjectSet;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.internal.reflect.Instantiator;
import xapi.fu.*;
import xapi.fu.itr.Chain;
import xapi.fu.itr.ChainBuilder;
import xapi.gradle.api.*;
import xapi.gradle.config.PlatformConfig;
import xapi.gradle.config.PlatformConfigContainer;
import xapi.gradle.config.PublishConfig;
import xapi.gradle.task.XapiInit;

import javax.annotation.Nonnull;
import java.util.concurrent.Callable;

import static xapi.fu.Out1.out1Unsafe;

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
    private final Property<String> module;
    private final Property<Boolean> autoPlugin;
    private final Property<PublishConfig> publish;
    private final Property<SourceConfigContainer> sources;
    private final Property<AllJars> jars;
    private final TaskProvider<XapiInit> initTask;
    private final DomainObjectSet<In1Out1<TaskProvider<XapiInit>, TaskProvider<?>>> beforeInit, afterInit;
    private final Out1<XapiInit> init;
    private final ChainBuilder<Do> onInit, onPrepare, onFinish;
    private final Logger logger;
    private final PlatformConfigContainer platform;
    private final Callable<String> defaultModule;
    private final Property<ArchiveType> mainArtifact;
    private final Lazy<MavenPublication> mainPublication;

    /*
     * TODO: make a bunch of helper methods:
     * requireDev
     * requireApi
     * requireGwt
     * etc etc.
     *
     * or, perhaps:
     *
     * xapi {
     *   // adding a main artifact to a main artifact
     *   require 'xapi-lang' // require adds to main
     *   // we may want a "dev" artifact in main
     *   require 'xapi-lang', 'dev'
     *   platform {
     *     gwt {
     *       // to add specific extras to a given platform:
     *       require 'xapi-lang', 'gwt'
     *       require 'xapi-lang' // maybe make this do `, 'gwt'` i.e. allow this line and the one above to be ==
     *       // in either case, both lines above should be redundant, as platform should automatically inherit classifiers
     *       // whenever there is a main `require()` on another main artifact.
     *     }
     *   }
     *   // to add specific extras to a specific artifact:
     *   requireApi 'xapi-lang', 'api'
     * }
     */

    public XapiExtension(Project project, Instantiator instantiator) {
        logger = project.getLogger();
        platform = new PlatformConfigContainer(project.getObjects(), instantiator);
        autoPlugin = lockable(project, Boolean.class);
        prefix = lockable(project, String.class);
        publish = lockable(project, PublishConfig.class);
        sources = lockable(project, SourceConfigContainer.class);
        jars = lockable(project, AllJars.class);
        mainArtifact = lockable(project, ArchiveType.class);
        mainArtifact.set(DefaultArchiveType.MAIN);

        module = lockable(project, String.class);
        defaultModule = ()->
            ((AbstractArchiveTask)project.getTasks().getByName(mainJarTask()))
                .getBaseName()
        ;
        module.set(project.provider(defaultModule));

        Lazy<SourceConfigContainer> lazySource = Lazy.deferred1(this::prepareSources, project, instantiator);
        sources.set(project.provider(lazySource::out1));
        Lazy<AllJars> lazyJars = Lazy.deferred1(this::prepareJars, project);
        jars.set(project.provider(lazyJars::out1));

        onInit = Chain.startChain();
        onPrepare = Chain.startChain();
        onFinish = Chain.startChain();
        // Add a sentinel value; if the list is empty, we will immediately execute the callback.
        onInit.add(Do.NOTHING);
        onPrepare.add(Do.NOTHING);
        onFinish.add(Do.NOTHING);

        beforeInit = new DefaultDomainObjectSet<In1Out1<TaskProvider<XapiInit>, TaskProvider<?>>>(Class.class.cast(In1Out1.class));
        afterInit = new DefaultDomainObjectSet<In1Out1<TaskProvider<XapiInit>, TaskProvider<?>>>(Class.class.cast(In1Out1.class));

        configureDefaults(project);

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
        mainPublication = Lazy.deferred1(PublishXapi::addPublishXapiTask, project);
    }

    protected String mainJarTask() {
        return JavaPlugin.JAR_TASK_NAME;
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

    protected SourceConfigContainer prepareSources(Project project, Instantiator instantiator) {
        return new SourceConfigContainer(project, instantiator);
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
        this.prefix.set(project.provider(()-> GradleServiceFinder.getService(project).guessPrefix()));
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
        // TODO: add onPrefix listeners, to execute immediately after the prefix is known.
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

        // let onPrepare callbacks edit publishing settings
        onPrepare.removeAll(Do.INVOKE);

        // finalize publishing...
        publish.finalizeValue();
        PublishConfig config = publish.get();

        if (!config.isHidden()) {
            jars.get().getSources().configure(sourceJar ->
                sourceJar.onlyIf(ignored->config.isSources())
            );
        }
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

    public Property<SourceConfigContainer> getSources() {
        return sources;
    }

    public SourceConfigContainer sources() {
        sources.finalizeValue();
        return sources.get();
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
//
//    public String exportSettings(XapiManifest manifest, ArchiveType type) {
//        DomBuffer out = new DomBuffer("xapi", false);
//        out.allowAbbreviation(true);
//        manifest.printMain(out, manifest, type);
//        return out.toSource();
//    }

    public Logger getLogger() {
        return logger;
    }

    public PlatformConfigContainer getPlatform() {
        return platform;
    }

    public void platform(Action<? super PlatformConfigContainer> container) {
        container.execute(getPlatform());
    }

    public void source(Action<? super SourceConfigContainer> container) {
        container.execute(sources());
    }

    public Property<String> getModule() {
        return module;
    }

    public String module() {
        module.finalizeValue();
        assert module.get().equals(out1Unsafe(defaultModule::call).out1()) :
            "Edited jar.baseName '" + out1Unsafe(defaultModule::call).out1() + "'; after xapi.module was finalized to '" + module.get() + "'";
        return module.get();
    }

    public Property<ArchiveType> getMainArtifact() {
        return mainArtifact;
    }

    public void setMainArtifact(Provider<ArchiveType> mainArtifact) {
        this.mainArtifact.set(mainArtifact);
    }

    public void setMainArtifact(ArchiveType mainArtifact) {
        this.mainArtifact.set(mainArtifact);
    }

    public void forArchiveTypes(In1<String> in1) {
        // Check w/ root extensions for common types as well...
        for (DefaultArchiveType value : DefaultArchiveType.values()) {
            if (value.isDocs()) {
                continue;
            }
            in1.in(value.sourceName());
        }
        for (ArchiveType value : PlatformType.all()) {
            in1.in(value.sourceName());
        }

    }

    public PlatformConfig findPlatform(String type) {
        return PlatformType.find(type)
                    .mapIfPresent(ArchiveType::sourceName)
                    .mapIfPresent(getPlatform()::maybeCreate)
                    .getOrNull();
    }

    @Nonnull
    public MavenPublication getMainPublication() {
        return mainPublication.out1();
    }

    public String getPrefix() {
        prefix.finalizeValue();
        return prefix.get();
    }
}
