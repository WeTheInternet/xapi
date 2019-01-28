package net.wti.gradle.internal.require.api;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.require.api.ArchiveRequest.ArchiveRequestType;
import net.wti.gradle.require.api.DependencyKey;
import net.wti.gradle.schema.api.XapiSchema;
import net.wti.gradle.schema.internal.ArchiveConfigInternal;
import net.wti.gradle.schema.internal.PlatformConfigInternal;
import net.wti.gradle.schema.internal.SourceMeta;
import net.wti.gradle.schema.plugin.XapiSchemaPlugin;
import net.wti.gradle.system.api.LazyFileCollection;
import net.wti.gradle.system.impl.DefaultLazyFileCollection;
import org.gradle.api.Action;
import org.gradle.api.Named;
import org.gradle.api.artifacts.*;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier;
import org.gradle.api.internal.artifacts.dependencies.DefaultSelfResolvingDependency;
import org.gradle.api.internal.artifacts.dsl.LazyPublishArtifact;
import org.gradle.api.internal.plugins.DefaultArtifactPublicationSet;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.internal.Actions;
import org.gradle.internal.component.external.model.DefaultModuleComponentIdentifier;
import org.gradle.jvm.tasks.Jar;
import org.gradle.util.GUtil;

import java.io.File;
import java.util.Map;
import java.util.Set;

import static org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE;

/**
 * TODO: rename to ModuleGraph
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 1/2/19 @ 4:23 AM.
 */
@SuppressWarnings("UnstableApiUsage")
public interface ArchiveGraph extends Named {
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

    default boolean srcExists() {
        // TODO: replace this with a .whenExists variation
        return srcRoot().exists();
    }

    default String getConfigName() {
        String platName = platform().getName();
        return
            "main".equals(platName)
                //            ||
                //            getName().equals(platName)
                ?
                getName() : platName + GUtil.toCamelCase(getName());

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
            runtime.extendsFrom(configAssembled());
            runtime.setVisible(false);
        });
    }

    @SuppressWarnings("deprecation")
    default Configuration configExportedApi() {
//        return config(JavaPlugin.API_ELEMENTS_CONFIGURATION_NAME, apiElements -> {
        return config("ExportCompile", apiElements -> {
            apiElements.extendsFrom(configAssembled());
            apiElements.setVisible(false);
            apiElements.setCanBeResolved(false);
            apiElements.setCanBeConsumed(true);
            apiElements.getOutgoing().capability(getCapability());
            withAttributes(apiElements)
                .attribute(USAGE_ATTRIBUTE, project().usageApi());


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
            exported.getOutgoing().capability(getCapability());
        });
    }

    default String getCapability() {
        return getGroup() + ":" + getModuleName() + ":" + getView().getVersion();
    }

    default Configuration configExportedRuntime() {
//            config(JavaPlugin.RUNTIME_ELEMENTS_CONFIGURATION_NAME).extendsFrom(runtimeElements);
        return config("ExportRuntime", runtimeElements -> {
            runtimeElements.extendsFrom(configInternal());
            runtimeElements.extendsFrom(configAssembled());
            runtimeElements.setVisible(false);
            runtimeElements.setCanBeResolved(false);
            runtimeElements.setCanBeConsumed(true);
            runtimeElements.getOutgoing().capability(getCapability());
            withAttributes(runtimeElements)
                .attribute(USAGE_ATTRIBUTE, project().usageRuntime());
        });
    }

    default AttributeContainer withAttributes(Configuration conf) {
        return conf.getAttributes()
                .attribute(XapiSchemaPlugin.ATTR_PLATFORM_TYPE, platform().getName())
                .attribute(XapiSchemaPlugin.ATTR_ARTIFACT_TYPE, getName());
    }

    @SuppressWarnings("deprecation")
    default Configuration configAssembled() {
        return config("Assembled", assembled -> {
            assembled.extendsFrom(configTransitive());
            assembled.extendsFrom(configApi());
        });
    }

    default Configuration configPackaged() {
        return config("Packaged", assembled -> {
            assembled.extendsFrom(configAssembled());
        });
    }

    /**
     * @return the java-plugin-compatible "api" configuration to use for declared dependencies.
     *
     * This is deprecated since you should use {@link #configTransitive()} instead.
     * It's just here so we can make sure we are compatible w/ java and java-library plugins.
     */
    @Deprecated
    default Configuration configApi() {
        String key = getSrcName() + "Api";
        if ("mainApi".equals(key)) {
            key = "api";
        }
        final Configuration api = getView().getConfigurations().findByName(key);
        if (api == null) {
            return config("Compile");
        } else {
            return config("Api");
        }
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
                configApi().extendsFrom(transitive);
            }
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
        String name = getName();
        return "main".equals(name) ? projName : projName + "-" + name;
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
                    config("CompileOnly").extendsFrom(intransitive);
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

        PublishArtifact jarArtifact = new LazyPublishArtifact(jarTask);
        view.getExtensions().getByType(DefaultArtifactPublicationSet.class).addCandidate(jarArtifact);

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

        configAssembled().extendsFrom(config);

        if (srcExists()) {
            view.getDependencies().add(
                name,
                allSourceDirs().getSourceDirectories()
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
            if (srcExists()) {
                config("Implementation").extendsFrom(internal);
            }
        });
    }

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    default Configuration configLenient(Configuration from) {
        return config(from.getName() + "Lenient", lenient -> {
            final ModuleIdentifier mid = getModuleIdentifier();
            final DefaultModuleComponentIdentifier compId = new DefaultModuleComponentIdentifier(
                DefaultModuleIdentifier.newId(mid.getGroup(), mid.getName() + "-" + from.getName().toLowerCase()),
                getView().getVersion()
            );
            // We don't add the lenient configuration directly to the parent; instead we add a dependency
            // which lets us route through a lazy, lenient file collection:
            LazyFileCollection lazy = new DefaultLazyFileCollection(getView(), lenient, true);
            // We might want a more specific subtype for this self resolving dependency, so we are detectable externally.
            configTransitive().getDependencies().add(new DefaultSelfResolvingDependency(compId, lazy));
            // The configuration object can be used for resolving dependencies, in a lazy fashion.
            lenient.setVisible(false);
            lenient.setCanBeConsumed(false);
            lenient.setCanBeResolved(true);
            final AttributeContainer fromAttrs = from.getAttributes();
            for (Attribute attribute : fromAttrs.keySet()) {
                lenient.getAttributes().attribute(attribute, fromAttrs.getAttribute(attribute));
            }

        });
    }

    default Configuration config(String suffix) {
        return config(suffix, Actions.doNothing());
    }

    default Configuration config(String suffix, Action<? super Configuration> whenCreated) {
        final ConfigurationContainer configs = getConfigurations();
        final String srcName = getSrcName();
        String name = "main".equals(srcName) || srcName.equals(suffix) || suffix.startsWith(srcName) ? GUtil.toLowerCamelCase(suffix) : srcName + GUtil.toCamelCase(suffix);
        Configuration existing = configs.findByName(name);
        if (existing == null) {
            existing = configs.create(name);
            whenCreated.execute(existing);
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

    default Configuration configImport(Configuration target, String suffix, Action<? super AttributeContainer> attrCallback) {
        String targetName = target.getName();
        String myName = getSrcName();
        String name = myName.equals(targetName) ? myName + "Import" + suffix : targetName + "Import" + suffix +
            (targetName.startsWith(myName) ? "" : GUtil.toCamelCase(getConfigName()));
        return config(name, conf->{
            target.extendsFrom(conf);
            // set platform/archive attributes on the configuration
            final AttributeContainer attrs = withAttributes(conf);
            conf.setCanBeResolved(true);
            conf.setCanBeConsumed(false);
            conf.setVisible(false);
            attrCallback.execute(attrs);
        });
    }
    default Configuration configImportApi(Configuration target) {
        return configImport(target, "Api", attributes->
            // gradle's PreferJavaRuntimeVariant defaults to runtime in order to maintain backwards compatibility
            // with maven-repo users, who expect runtime transitive dependencies.
            // We do not need to maintain any backwards compatibility, so we'll use api for now, and make configurable later.
            attributes.attribute(USAGE_ATTRIBUTE, project().usageApi())
        );
    }
    default Configuration configImportRuntime(Configuration target) {
        return configImport(target, "Runtime", attributes->
            attributes.attribute(USAGE_ATTRIBUTE, project().usageRuntime())
        );
    }
}
