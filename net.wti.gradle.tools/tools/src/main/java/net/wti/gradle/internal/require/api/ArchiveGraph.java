package net.wti.gradle.internal.require.api;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.api.ReadyState;
import net.wti.gradle.internal.impl.IntermediateJavaArtifact;
import net.wti.gradle.internal.require.api.ArchiveRequest.ArchiveRequestType;
import net.wti.gradle.require.api.DependencyKey;
import net.wti.gradle.require.api.PlatformModule;
import net.wti.gradle.schema.api.Transitivity;
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
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.attributes.Bundling;
import org.gradle.api.attributes.java.TargetJvmVersion;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.artifacts.ArtifactAttributes;
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier;
import org.gradle.api.internal.file.FileCollectionInternal;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.internal.Actions;
import org.gradle.util.GUtil;
import xapi.gradle.fu.LazyString;

import javax.annotation.Nullable;
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
                .attribute(ArtifactAttributes.ARTIFACT_FORMAT, ArtifactTypeDefinition.JAR_TYPE)
//                .attribute(USAGE_ATTRIBUTE, project().usageApi())
            ;
            configNamed("default", def->def.extendsFrom(exportCompile));
            if (config().isSourceAllowed()) {
                exportCompile.extendsFrom(configSource());
            }
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
            internal.getOutgoing().capability(getCapability("internal"));
            withAttributes(internal)
                .attribute(USAGE_ATTRIBUTE, project().usageInternal())
                .attribute(ArtifactAttributes.ARTIFACT_FORMAT, ArtifactTypeDefinition.JAR_TYPE)
            ;
        });
    }
    default Configuration configExportedSource() {
        return config("ExportSource", internal -> {
            internal.setVisible(true);
            internal.setCanBeResolved(true);
            internal.setCanBeConsumed(true);
            internal.getOutgoing().capability(getCapability("sources"));
            withAttributes(internal)
                .attribute(USAGE_ATTRIBUTE, project().usageSourceJar())
                .attribute(ArtifactAttributes.ARTIFACT_FORMAT, ArtifactTypeDefinition.JAR_TYPE)
            ;
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
                .attribute(ArtifactAttributes.ARTIFACT_FORMAT, ArtifactTypeDefinition.JAR_TYPE)
            ;
            configNamed("default", def->def.extendsFrom(exportRuntime));

//            if (config().isSourceAllowed()) {
//                // runtime will depend on the exported source jar.
//                exportRuntime.extendsFrom(configSource());
//            }
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
        return platform().getMainModule().getModuleName();
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
                .attribute(XapiSchemaPlugin.ATTR_ARTIFACT_TYPE, getName())
                .attribute(Bundling.BUNDLING_ATTRIBUTE, project().bundlingExternal())
                .attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 8)
                ;
    }

    @SuppressWarnings("deprecation") // configApi() is deprecated only for external use cases
    default Configuration configTransitive() {
        return config("Transitive", transitive-> {
            platform().configGlobal().extendsFrom(transitive);
            // This configuration is only for declaring dependencies, not resolving them.
            transitive.setVisible(false);
            transitive.setCanBeConsumed(false);
            transitive.setCanBeResolved(false);
            if (realized()) {
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

        config = configs.maybeCreate(name);

        configExportedApi().extendsFrom(config);

        if (srcExists()) {
            final TaskProvider<Jar> sourceJarTask = getSourceJarTask();
            final ConfigurationPublications outgoing = configExportedSource().getOutgoing();
            final FileCollectionInternal srcDirs = (FileCollectionInternal) allSourceDirs().getSourceDirectories();
            final FileCollection filtered = srcDirs.filter(File::exists);
            if (!filtered.isEmpty()) {
                final Jar srcJarTask = sourceJarTask.get();
                IntermediateJavaArtifact sourceJar = new IntermediateJavaArtifact("sources", srcJarTask) {
                    @Override
                    public File getFile() {
                        return srcJarTask.getArchiveFile().get().getAsFile();
                    }

                    @Override
                    public String getExtension() {
                        return "jar";
                    }

                    @Nullable
                    @Override
                    public String getClassifier() {
                        // blech.  maven publications require this to be a classifier'd artifact.
                        // what we should really be doing is N publications, for the -sources, or -compile variants...
                        return "sources";
                    }
                };
                outgoing.variants(variants->{
                    // TODO: distinguish between sources and source-jars
                    final ConfigurationVariant variant = variants.maybeCreate("sources");
                    variant.artifact(sourceJar);
                    variant.attributes(attrs->{
                        withAttributes(attrs);
                        attrs.attribute(XapiSchemaPlugin.ATTR_ARTIFACT_TYPE, getModuleName() + "-sources");
                        attrs.attribute(USAGE_ATTRIBUTE, project().usageSource());
                    });
                });

                outgoing.artifact(sourceJar);

                view.getDependencies().add(
                    name,
                    filtered
                );
            }
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

    default TaskProvider<Jar> getSourceJarTask() {
        return getTasks().getSourceJarTask();
    }

    default TaskProvider<JavaCompile> getJavacTask() {
        return getTasks().getJavacTask();
    }

    default void importLocal(ArchiveGraph neededArchive, Transitivity transitivity, boolean lenient) {
        // TODO: smartly transform / restrict the addition of Api/Runtime dependencies.
        importLocal(neededArchive, DefaultUsageType.Api, transitivity, lenient);
        importLocal(neededArchive, DefaultUsageType.Runtime, transitivity, lenient);
    }

    default void importLocal(ArchiveGraph neededArchive, UsageType type, Transitivity trans, boolean lenient) {
        // TODO: instead create an ArchiveRequest here,
        //  so we can avoid doing this wiring if we don't need to;
        //  i.e. modules w/out sources or outputs could be "ghosted", with only a minimal presence,
        //  by creating a request whose needsCompletion is never set to true.

        final ProjectGraph project = project();

        final Configuration consumer = type.findConsumerConfig(this, trans);
        final ModuleDependency md = type.newDependency(project, this, neededArchive, trans);

        getView().getLogger().trace("{} adding dependency {} to {}",
            new LazyString(this::getModuleName), md, consumer.getName());
        consumer.getDependencies().add(md);
    }

    default void importProject(
        String projName,
        String platName,
        String modName,
        Transitivity only,
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
        String classifier,
        Transitivity trans,
        boolean lenient
    ) {
        if (reg.getFrom() != null && "sources".equals(classifier)) {
            importExternal(dep, reg, DefaultUsageType.Source, newGroup, newName, classifier, trans, lenient);
        } else {
            importExternal(dep, reg, DefaultUsageType.Api, newGroup, newName, classifier, trans, lenient);
            importExternal(dep, reg, DefaultUsageType.Runtime, newGroup, newName, classifier, trans, lenient);
        }
    }
    @SuppressWarnings({"unchecked", "ConstantConditions"})
    default void importExternal(
        Dependency dep,
        XapiRegistration reg,
        UsageType type,
        String newGroup,
        String newName,
        String classifier,
        Transitivity trans,
        boolean lenient
    ) {
        // TODO: move all the import* logic from ArchiveGraph, into a new class, DependencyStitcher

        final DependencyHandler deps = getView().getDependencies();
        final XapiSchema schema = getView().getSchema();
        final String coords = GradleCoerce.unwrapString(reg.getFrom());
        final AttributeContainer attrs = schema.getAttributes(coords, newGroup, newName);

        // create variant-aware, on-demand inputs to the lenient config.
        Configuration target = type.findConsumerConfig(this, trans);

        boolean hasXapiCoord = coords != null;// && "true".equals(System.getProperty("no.composite"));

        final String ident = dep.getGroup() + ":" + dep.getName();
        final String path = ident + ":" + dep.getVersion() +
            (classifier == null ? "" : ":" + classifier);
        String base;
        if (hasXapiCoord) {
            String[] parts = coords.split(":");
            if (parts.length == 2) {
                if ("main".equals(parts[0])) {
                    base = parts[1];
                } else if ("main".equals(parts[1])) {
                    base = parts[0];
                } else {
                    base = parts[0] + GUtil.toCamelCase(parts[1]);
                }
            } else if (parts.length == 1) {
                base = "main".equals(parts[0]) ? "main" : "main" + GUtil.toCamelCase(parts[0]);
            } else {
                assert parts.length == 0 : "Invalid xapi coordinates " + coords;
                base = "main";
            }
        } else {
            // no coords?
            base = attrs.getAttribute(XapiSchemaPlugin.ATTR_PLATFORM_TYPE);
            if ("main".equals(base)) {
                base = attrs.getAttribute(XapiSchemaPlugin.ATTR_ARTIFACT_TYPE);
            } else {
                final String toAdd = GUtil.toCamelCase(attrs.getAttribute(XapiSchemaPlugin.ATTR_ARTIFACT_TYPE));
                if (!"Main".equals(toAdd)) {
                    base += toAdd;
                }
            }
        }
        ModuleDependency mod = (ModuleDependency) deps.create(path);

        if (coords == null) {
            // An external w/out additional xapiCoords
            mod.setTargetConfiguration(
                type.deriveConfiguration(base, dep, false, trans, lenient)
            );
        } else {
            String[] bits = coords.split(":");
            assert bits.length <3 : "Invalid Xapi descriptor contains more than one : character -> " + coords;
            final String fromPlat = bits.length == 1 ? "main" : bits[0];
            String fromMod = bits[bits.length-1];
            if (fromMod.isEmpty()) {
                fromMod = "main";
            }
            String require = type.computeRequiredCapabilities(dep, fromPlat, fromMod, classifier);
            getView().getLogger().debug("Got require {} on ident {} for {} on {} coords {}", require, ident, dep , base, coords);
            if (!require.isEmpty()
                && !(require + ":").startsWith(ident + ":")
            ) {
                if (getView().isWtiGradle()) {
                    // This relies on WTI's fork of gradle.
                    final String derived = type.deriveConfiguration(base, dep, hasXapiCoord, trans, lenient);
                    if (derived == null) {
                        if (require.endsWith("-sources")) {
                            // dirty hack for now... we'll want to register each variant type in a way that allows
                            // them to contribute dependency attributes wherever it makes sense to do so.
                            mod.attributes(depAttrs ->
                                depAttrs.attribute(USAGE_ATTRIBUTE, project().usageSourceJar())
                                        .attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 8)
                                        .attribute(Bundling.BUNDLING_ATTRIBUTE, getView().getObjects().named(Bundling.class, Bundling.EXTERNAL))
                            );
                            mod.capabilities(cap->cap.requireCapabilities(require + ":" + dep.getVersion()));
                        } else {
                            mod.capabilities(cap->cap.requireCapabilities(require + ":" + dep.getVersion()));
                        }
                    } else {
                        if ("sources".equals(classifier)) {
                            mod.getArtifacts().clear();
                            String finalMod = fromMod;
                            mod.attributes(depAttrs->
                                depAttrs
                                .attribute(XapiSchemaPlugin.ATTR_PLATFORM_TYPE, fromPlat)
                                .attribute(XapiSchemaPlugin.ATTR_ARTIFACT_TYPE, finalMod)
                                .attribute(USAGE_ATTRIBUTE, project().usageSourceJar())
                                .attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 8)
                                .attribute(Bundling.BUNDLING_ATTRIBUTE, getView().getObjects().named(Bundling.class, Bundling.EXTERNAL))
                            );
                        }
                        mod.setTargetConfiguration(derived);
                    }
                    getView().getLogger().debug("Derived {} for {} on {} coords {}", derived, dep , base, coords);
                } else {
                    // using requireCapabilities results in runtime jars instead of compiled classpaths, so it is less ideal.
                    // also; we need to get better version information here.  This should come from whatever-is-handling schema.xapi
                    mod.
                        capabilities(cap->cap.requireCapabilities(require + ":" + dep.getVersion()));

                }
            }
        }


        target.getDependencies().add(mod);
    }

    default String getDefaultPlatform() {
        return getView().getSchema().getMainPlatformName();
    }

    default String getDefaultModule() {
        return platform().config().getMainModuleName();
    }

    default PlatformModule asCoords() {
        return new PlatformModule(platform().getName(), getName());
    }
}
