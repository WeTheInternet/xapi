package net.wti.gradle.internal.require.api;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.api.ReadyState;
import net.wti.gradle.internal.require.api.ArchiveRequest.ArchiveRequestType;
import net.wti.gradle.require.api.DependencyKey;
import net.wti.gradle.schema.api.XapiSchema;
import net.wti.gradle.schema.internal.ArchiveConfigInternal;
import net.wti.gradle.schema.internal.PlatformConfigInternal;
import net.wti.gradle.schema.internal.SourceMeta;
import net.wti.gradle.schema.internal.XapiRegistration;
import net.wti.gradle.schema.plugin.XapiSchemaPlugin;
import net.wti.gradle.system.service.GradleService;
import net.wti.gradle.system.tools.GradleCoerce;
import org.gradle.api.Action;
import org.gradle.api.Named;
import org.gradle.api.artifacts.*;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier;
import org.gradle.api.internal.file.FileCollectionInternal;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.internal.Actions;
import org.gradle.util.GUtil;
import xapi.gradle.fu.LazyString;

import java.io.File;
import java.util.Map;
import java.util.Set;

import static net.wti.gradle.system.tools.GradleCoerce.isEmpty;
import static org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE;

/**
 * TODO: rename to ModuleGraph
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 1/2/19 @ 4:23 AM.
 */
@SuppressWarnings("UnstableApiUsage")
public interface ArchiveGraph extends Named, GraphNode {
    PlatformGraph platform();

    default ProjectGraph project() {
        return platform().project();
    }

    ArchiveRequest request(ArchiveGraph other, ArchiveRequestType type);

    /**
     * @return group:name:version / whatever we need to create external dependencies.
     * <p>
     * If you are creating an archive graph from external module dependencies,
     * then this map contains either group:name:version:changing:etc,
     * or is structured as a "deep map":
     * `{composite: [{g:n:v:c:etc}, {...}]`
     * <p>
     * When an archive graph is being created from project sources,
     * then this map will have `{external:false, g:n:v:etc:...}`,
     * and it will control the publishing destination / external lookup of this graph node.
     */
    Map<DependencyKey, ?> id();

    /**
     * @return The location of the source set directory;
     * If you have a project `projName`, a platform of `plat` and archive type of `arch`,
     * then the conventional value of this property is roughly:
     * `$rootDir/projName/src/platArch`
     */
    File srcRoot();

    ModuleTasks getTasks();

    DependencyStitcher getDependencies();

    default boolean srcExists() {
        // TODO: replace this with a .whenExists variation
        return srcRoot().exists();
    }

    default String getConfigName() {
        String platName = platform().getName();
        return
            "main".equals(platName)
                ? getName() : platName + GUtil.toCamelCase(getName());

    }

    default String getSrcName() {
        String name = getConfigName().replace("Main", "");
        return name.isEmpty() ? platform().getName() : name;
    }

    Set<ArchiveRequest> getIncoming();

    Set<ArchiveRequest> getOutgoing();

    default boolean hasIncoming() {
        return getIncoming().stream().anyMatch(ArchiveRequest::isSelectable);
    }

    // this node is used as a dependency
    default boolean hasOutgoing() {
        return getOutgoing().stream().anyMatch(ArchiveRequest::isSelectable);
    }

    default boolean realized() {
        return srcExists() || hasIncoming() || hasOutgoing();
    }

    default boolean isSelectable() {
        return platform().isSelectable();
    }

    default ConfigurationContainer getConfigurations() {
        return project().project().getConfigurations();
    }

    default Configuration configRuntime() {
        return config("Runtime", runtime-> {
            runtime.extendsFrom(configInternal());
            runtime.setVisible(false);
        });
    }

    default Configuration configRuntimeOnly() {
        return config("RuntimeOnly", runtime-> {
            configRuntime().extendsFrom(runtime);
            runtime.setVisible(false);
            runtime.setCanBeConsumed(false);
            runtime.setCanBeResolved(false);
        });
    }
    default Configuration configCompileOnly() {
        return config("CompileOnly", compOnly -> {
            configCompile().extendsFrom(compOnly);
//            configRuntime().extendsFrom(runtime);
//            runtime.setVisible(false);
//            runtime.setCanBeConsumed(false);
//            runtime.setCanBeResolved(false);
        });
    }

    default Configuration configCompile() {
        // nasty... if we try to use built-in compile or api configurations,
        // then the implicit `runtime.extendsFrom compile` entry will mess up
        // our (current, ugly) resolution rules.
        // So, instead, we directly attach our srcSetCompilePath to the srcSetCompileClasspath,
        // and do not interface directly with java plugin's transitive configurations.
        // If the user wants to attach our configurations to standard java, it is up to them
        // to decide on a case-by-case basis where they want an arbitrary xapi dependency inserted.
        return config("CompilePath", comp -> {
            config("CompileClasspath").extendsFrom(comp);
            // these match java plugin's defaults for CompileClasspath, but we want to be explicit here,
            // as we intend to operate w/out the java plugin at all.
            comp.setVisible(true);
            comp.setCanBeConsumed(true);
            comp.setCanBeResolved(true);
            config("CompileTest", test->test.extendsFrom(comp));
        });
    }

    @SuppressWarnings("deprecation")
    default Configuration configExportedApi() {
//        return config(JavaPlugin.API_ELEMENTS_CONFIGURATION_NAME, apiElements -> {
        return config("ExportCompile", exportCompile -> {
            exportCompile.extendsFrom(configTransitive());
            exportCompile.setVisible(true);
            exportCompile.setDescription("The compile output and transitive dependencies for " + getConfigName());
            exportCompile.setCanBeResolved(true);
            exportCompile.setCanBeConsumed(true);
            exportCompile.getOutgoing().capability(getCapability("compile"));
            withAttributes(exportCompile)
//                .attribute(USAGE_ATTRIBUTE, project().usageApi())
            ;
            configNamed("default", def->def.extendsFrom(exportCompile));
        });
    }

    default String getVersion() {
        return platform().getVersion();
    }

    default Configuration configExportedInternal() {
        return config("ExportInternal", internal -> {
            internal.setVisible(true);
            internal.setCanBeResolved(true);
            internal.setCanBeConsumed(true);
//            internal.getOutgoing().capability(getCapability("internal"));
            withAttributes(internal)
                .attribute(USAGE_ATTRIBUTE, project().usageInternal());
        });
    }

    default Configuration configExportedRuntime() {
//            config(JavaPlugin.RUNTIME_ELEMENTS_CONFIGURATION_NAME).extendsFrom(runtimeElements);
        return config("ExportRuntime", exportRuntime -> {
            exportRuntime.extendsFrom(configInternal());
            exportRuntime.setVisible(false);
            exportRuntime.setCanBeConsumed(true);
            exportRuntime.setCanBeResolved(false);
            exportRuntime.getOutgoing().capability(getCapability());

            withAttributes(exportRuntime)
                .attribute(USAGE_ATTRIBUTE, project().usageRuntime())
            ;
            configNamed("default", def->def.extendsFrom(exportRuntime));
        });
    }

    default Configuration configExported() {
        // Takes on the role of "all output artifacts" from the "default" configuration.
        // This is what you should depend on, in a configuration with appropriate variant attributes setup.
        return config("Out", exported-> {
            exported.extendsFrom(configExportedApi());
            exported.extendsFrom(configExportedRuntime());
            exported.setVisible(true);
            exported.setCanBeResolved(true);
            exported.setCanBeConsumed(true);
//            exported.getOutgoing().capability(getCapability("all"));
        });
    }

    default String getCapability() {
        return getGroup() + ":" + getModuleName() + ":" + getView().getVersion();
    }

    default String getCapabilityCore() {
        return project().getGroup() + ":" + getNameCore() + ":" + getView().getVersion();
    }

    default String getNameCore() {
        return platform().getMainModule();
    }

    default String getCapability(String suffix) {
        return getCapability(suffix, true);
    }

    default String getCapability(String suffix, boolean withVersion) {
        assert suffix != null;
        return getGroup() +
            ":" + getModuleName() + (suffix.isEmpty() || suffix.startsWith("-") ? suffix : "-" + suffix) +
            (withVersion ? ":" + getVersion() : "");
    }

    default AttributeContainer withAttributes(Configuration conf) {
        return withAttributes(conf.getAttributes());
    }

    default AttributeContainer withAttributes(AttributeContainer attrs) {
        return attrs.attribute(XapiSchemaPlugin.ATTR_PLATFORM_TYPE, platform().getName())
                .attribute(XapiSchemaPlugin.ATTR_ARTIFACT_TYPE, getName());
    }

    @SuppressWarnings("deprecation") // configApi() is deprecated only for external use cases
    default Configuration configTransitive() {
        return config("Transitive", transitive-> {
            platform().configGlobal().extendsFrom(transitive);
            // This configuration is only for declaring dependencies, not resolving them.
            transitive.setVisible(false);
            transitive.setCanBeConsumed(false);
            transitive.setCanBeResolved(false);
            if (srcExists()) {
                configCompile().extendsFrom(transitive);
                config("compile", transitive::extendsFrom);
                config("api", transitive::extendsFrom);
            }
//            withAttributes(transitive);
        });
    }

    default ModuleIdentifier getModuleIdentifier() {
        return DefaultModuleIdentifier.newId(getGroup(), getModuleName());
    }

    default String asGroup(String group) {
        return platform().asGroup(group);
    }

    default String getGroup() {
        return platform().getGroup();
    }

    default String asModuleName(String projName) {
        final String name = getName();
        final String platName = platform().getName();
        return XapiNamer.moduleName(projName, platName, name);
    }

    default String getModuleName() {
        String projName = getView().getName();
        return asModuleName(projName);
    }

    default Configuration configIntransitive() {
        return config("Intransitive"
            , intransitive -> {
                intransitive.setDescription(getPath() + " compile-only dependencies");
                intransitive.setCanBeResolved(false);
                intransitive.setCanBeConsumed(false);
                intransitive.setVisible(false);
                if (srcExists()) {
                    configCompileOnly().extendsFrom(intransitive);
                }
            }
        );
    }

    default Configuration configSource() {
        final ProjectView view = getView();
        XapiSchema schema = view.getSchema();
        String name = getSrcName() + "Source";
        ConfigurationContainer configs = view.getConfigurations();
        Configuration config = configs.findByName(name);
        if (config != null) {
            return config;
        }

        String taskName = name + "Jar";
        final TaskContainer tasks = view.getTasks();
        config = configs.maybeCreate(name);

        final TaskProvider<Jar> jarTask = tasks.register(taskName, Jar.class, jar -> {
            if (srcExists()) {
                SourceMeta meta = getSource();
                jar.from(meta.getSrc().getAllSource());
                jar.getExtensions().add(SourceMeta.EXT_NAME, meta);
            }
            if (schema.getArchives().isWithCoordinate()) {
                assert !schema.getArchives().isWithClassifier() : "Archive container cannot use both coordinate and classifier: " + schema.getArchives();
                jar.getArchiveAppendix().set("sources");
            } else {
                jar.getArchiveClassifier().set("sources");
            }
        });

        final ConfigurationPublications outgoing = config.getOutgoing();

//        PublishArtifact jarArtifact = new LazyPublishArtifact(jarTask, getVersion());
//        view.getExtensions().getByType(DefaultArtifactPublicationSet.class).addCandidate(jarArtifact);

        // Now that we have the rest of variants working, we should be able to reinstate this...
        //        outgoing.variants(variants->{
        //            final ConfigurationVariant variant = variants.maybeCreate("sources");
        //            variant.attributes(attrs->
        //                attrs.attribute(XapiSchemaPlugin.ATTR_ARTIFACT_TYPE, "sources")
        //            );
        ////        if (hasSrc) {
        ////            for (File srcDir : archive.allSourceDirs().getSourceDirectories()) {
        ////                final Provider<File> provider = providers.provider(() -> srcDir);
        ////                LazyPublishArtifact src = new LazyPublishArtifact(provider);
        ////                final Directory proj = project.getLayout().getProjectDirectory();
        ////                final String seg = srcDir.getAbsolutePath().replace(proj.getAsFile().getAbsolutePath(), "").substring(1);
        ////                final Directory asDir = proj.dir(seg);
        ////                outgoing.artifact(new FileSystemPublishArtifact(
        ////                    asDir, project.getVersion()
        ////                ));
        //////                outgoing.artifact(src);
        ////            }
        ////        }
        //        });

//        configAssembled().extendsFrom(config);
        configExportedApi().extendsFrom(config);

        if (srcExists()) {
            final FileCollectionInternal srcDirs = (FileCollectionInternal) allSourceDirs().getSourceDirectories();
//            final ComponentIdentifier srcId = getComponentId("source");
//            final DefaultSelfResolvingDependency dep = new DefaultSelfResolvingDependency(srcId, srcDirs);
            view.getDependencies().add(
                name,
                srcDirs
//                dep
            );
        }
        final PlatformGraph platform = platform();
        final PlatformConfigInternal parent = platform.config().getParent();
        if (parent != null) {
            // If there's a parent platform, we want to depend on the sources configuration of said parent.
            final PlatformGraph parentPlatform = view.getProjectGraph().platform(parent.getName());
            final ArchiveGraph parentArchive = parentPlatform.archive(getName());
            // now, to wire up the source configs...
            config.extendsFrom(parentArchive.configSource());
        }
        return config;
    }

    default String getPath() {
        return platform().getPath() + ":" + getName();
    }

    default Configuration configInternal() {
        return config("Internal", internal -> {
            internal.setVisible(false);
            internal.setCanBeConsumed(false);
            internal.setCanBeResolved(false);
            internal.extendsFrom(configTransitive());
            configCompile().extendsFrom(internal);
        });
    }

    ModuleComponentIdentifier getComponentId(String name);

    default Configuration config(String suffix) {
        return config(suffix, Actions.doNothing());
    }

    default Configuration config(String suffix, Action<? super Configuration> whenCreated) {
        final String srcName = getSrcName();
        String name = "main".equals(srcName) || srcName.equals(suffix) || suffix.startsWith(srcName) ? GUtil.toLowerCamelCase(suffix) : srcName + GUtil.toCamelCase(suffix);
        return configNamed(name, whenCreated);
    }

    default Configuration configNamed(String name, Action<? super Configuration> whenCreated) {
        final ConfigurationContainer configs = getConfigurations();
        Configuration existing = configs.findByName(name);
        if (existing == null) {
            existing = configs.create(name);
        }
        if (existing instanceof ExtensionAware){
            // we want some else-ish semantics where we detect already-existing configurations.
            // since we'll also want those for items we do create, we take liberal advantage of ExtensionAware
            // to tag each Configuration with a per-ArchiveGraph key to decide when to run the given callback.
            // We may eventually route this key to a method parameter, in case we want to give run-once-by-key
            // semantics to calling code.
            Configuration item = existing;
            GradleService.buildOnce((ExtensionAware)existing, "runOnce" + getSrcName(), ignored->{
                whenCreated.execute(item);
                return "";
            });
        }
        return existing;
    }

    default ProjectView getView() {
        return project().project();
    }

    default SourceMeta getSource() {
        return platform().sourceFor(getView().getSourceSets(), this);
    }

    default ArchiveConfigInternal config() {
        return platform().config().getArchives()
            .maybeCreate(getName());
    }

    default SourceDirectorySet allSourceDirs() {
        return getSource().getSrc().getAllSource();
    }

    default TaskProvider<Jar> getJarTask() {
        return getTasks().getJarTask();
    }

    default TaskProvider<JavaCompile> getJavacTask() {
        return getTasks().getJavacTask();
    }

    default void importLocal(ArchiveGraph neededArchive, boolean only, boolean lenient) {
        importLocal(neededArchive, DefaultUsageType.Api, only, lenient);
        importLocal(neededArchive, DefaultUsageType.Runtime, only, lenient);
    }

    default void importLocal(ArchiveGraph neededArchive, UsageType type, boolean only, boolean lenient) {
        // TODO: instead create an ArchiveRequest here,
        //  so we can avoid doing this wiring if we don't need to;
        //  i.e. modules w/out sources or outputs could be "ghosted", with only a minimal presence,
        //  by creating a request whose needsCompletion is never set to true.

        final ProjectGraph project = project();

        final Configuration consumer = type.findConsumerConfig(this, only);
        final ModuleDependency md = type.newDependency(project, this, neededArchive, only);

        getView().getLogger().trace("{} adding dependency {} to {}",
            new LazyString(this::getModuleName), md, consumer.getName());
        consumer.getDependencies().add(md);
    }

    default void importProject(
        String projName,
        String platName,
        String modName,
        boolean only,
        boolean lenient
    ) {
        final ProjectView self = getView();
        if (isEmpty(platName)) {
            platName = platform().getName();
        }
        if (isEmpty(modName)) {
            modName = getName();
        }
        assert !projName.equals(self.getPath()) : projName + " performed illegal importProject() on itself.  " +
            "Use .internal '" + platName + ":" + modName + "' instead.";
        final ProjectGraph targetProject = self.getBuildGraph().getProject(projName);
        final PlatformGraph targetPlatform = targetProject.platform(platName);
        final ArchiveGraph into = targetPlatform.archive(modName);
        // TODO: consider extra signifigance when modName or platName are empty (i.e. use a wider target like "all modules", "all platforms", etc.)

        targetPlatform.whenReady(ReadyState.BEFORE_FINISHED, ready->{

            importLocal(into, DefaultUsageType.Api, only, lenient);
            importLocal(into, DefaultUsageType.Runtime, only, lenient);

        });

    }

    default void importExternal(
        Dependency dep,
        XapiRegistration reg,
        String newGroup,
        String newName,
        boolean only,
        boolean lenient
    ) {
        importExternal(dep, reg, DefaultUsageType.Api, newGroup, newName, only, lenient);
        importExternal(dep, reg, DefaultUsageType.Runtime, newGroup, newName, only, lenient);
    }
    @SuppressWarnings({"unchecked", "ConstantConditions"})
    default void importExternal(
        Dependency dep,
        XapiRegistration reg,
        UsageType type,
        String newGroup,
        String newName,
        boolean only,
        boolean lenient
    ) {
        // TODO: move all the import* logic from ArchiveGraph, into a new class, DependencyStitcher

        final DependencyHandler deps = getView().getDependencies();
        final XapiSchema schema = getView().getSchema();
        final AttributeContainer attrs = schema.getAttributes(dep, newGroup, newName);

        // create variant-aware, on-demand inputs to the lenient config.
        Configuration target = type.findConsumerConfig(this, only);

        String from = GradleCoerce.unwrapString(reg.getFrom());

        boolean isLocal = from != null;// && "true".equals(System.getProperty("no.composite"));

        final String ident = dep.getGroup() + ":" + dep.getName();
        final String path = ident + ":" + dep.getVersion();
        String base = attrs.getAttribute(XapiSchemaPlugin.ATTR_PLATFORM_TYPE);
        if ("main".equals(base)) {
            base = attrs.getAttribute(XapiSchemaPlugin.ATTR_ARTIFACT_TYPE);
        } else {
            final String toAdd = GUtil.toCamelCase(attrs.getAttribute(XapiSchemaPlugin.ATTR_ARTIFACT_TYPE));
            if (!"Main".equals(toAdd)) {
                base += toAdd;
            }
        }
        ModuleDependency mod = (ModuleDependency) deps.create(path);

        if (from != null) {
            String[] bits = from.split(":");
            assert bits.length <3 : "Invalid Xapi descriptor contains more than one : character -> " + from;
            final String fromPlat = bits.length == 1 ? "main" : bits[0];
            String fromMod = bits[bits.length-1];
            if (fromMod.isEmpty()) {
                fromMod = "main";
            }
            String require = type.computeRequiredCapabilities(dep, fromPlat, fromMod);
            if (!require.isEmpty()
                && !(require + ":").startsWith(ident + ":")
            ) {
                if (getView().isWtiGradle()) {
                    // This relies on WTI's fork of gradle.
                    mod.setTargetConfiguration(
                        type.deriveConfiguration(base, dep, isLocal, only, lenient)
                    );
                } else {
                    // using requireCapabilities results in runtime jars instead of compiled classpaths, so it is less ideal.
                    // also; we need to get better version information here.  This should come from whatever-is-handling schema.xapi
                    mod.capabilities(cap->cap.requireCapabilities(require + ":" + dep.getVersion()));

                }
            }
        }

        if (from == null) {
            mod.setTargetConfiguration(
                type.deriveConfiguration(base, dep, false, only, lenient)
            );
        }

//        if (":xapi-collect".equals(getView().getPath())) {
//            System.out.println("\n\n\nFrom "+ mod.getName() + " : " + mod.getTargetConfiguration());
//        }

        target.getDependencies().add(mod);
    }

    default String getDefaultPlatform() {
        return getView().getSchema().getMainPlatformName();
    }

    default String getDefaultModule() {
        return platform().config().getMainModuleName();
    }
}
