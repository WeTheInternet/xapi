package net.wti.gradle.internal.require.api;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.require.api.ArchiveRequest.ArchiveRequestType;
import net.wti.gradle.require.api.DependencyKey;
import net.wti.gradle.schema.api.XapiSchema;
import net.wti.gradle.schema.internal.ArchiveConfigInternal;
import net.wti.gradle.schema.internal.PlatformConfigInternal;
import net.wti.gradle.schema.internal.SourceMeta;
import net.wti.gradle.system.api.LazyFileCollection;
import net.wti.gradle.system.impl.DefaultLazyFileCollection;
import org.gradle.api.Action;
import org.gradle.api.Named;
import org.gradle.api.artifacts.*;
import org.gradle.api.attributes.Usage;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier;
import org.gradle.api.internal.artifacts.dependencies.DefaultSelfResolvingDependency;
import org.gradle.api.internal.artifacts.dsl.LazyPublishArtifact;
import org.gradle.api.internal.plugins.DefaultArtifactPublicationSet;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.internal.Actions;
import org.gradle.internal.component.external.model.DefaultModuleComponentIdentifier;
import org.gradle.jvm.tasks.Jar;
import org.gradle.util.GUtil;

import java.io.File;
import java.util.Map;
import java.util.Set;

import static org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE;

/**
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
        return config(JavaPlugin.API_ELEMENTS_CONFIGURATION_NAME, apiElements -> {
            apiElements.extendsFrom(configApi());
            apiElements.setVisible(false);
            apiElements.setCanBeResolved(false);
            apiElements.setCanBeConsumed(true);
            apiElements.getAttributes().attribute(USAGE_ATTRIBUTE, getView().getObjects().named(Usage.class, Usage.JAVA_API));
        });
    }

    default Configuration configExportedRuntime() {
        return config(JavaPlugin.RUNTIME_ELEMENTS_CONFIGURATION_NAME, apiElements -> {
            apiElements.extendsFrom(configInternal());
            apiElements.setVisible(false);
            apiElements.setCanBeResolved(false);
            apiElements.setCanBeConsumed(true);
            apiElements.getAttributes().attribute(USAGE_ATTRIBUTE, getView().getObjects().named(Usage.class, Usage.JAVA_RUNTIME));
        });
    }

    default Configuration configAssembled() {
        return config("Assembled", assembled -> {
            assembled.extendsFrom(configTransitive());
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
        final Configuration api = getView().getConfigurations().findByName(getSrcName() + "Api");
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

    default Configuration configTransitiveLenient() {
        return configLenient(configTransitive());
    }

    default ModuleIdentifier getModuleIdentifier() {
        return DefaultModuleIdentifier.newId(getGroup(), getModuleName());
    }

    default String getGroup() {
        final String platId = platform().getName();
        String group = project().getGroup();
        if ("main".equals(platId)) {
            return group;
        }
        return group + "." + platId;
    }

    default String getModuleName() {
        String name = getName();
        String projName = getView().getName();
        return "main".equals(name) ? projName : projName + "-" + name;
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
            // The configuration object is for resolving dependencies, in a lazy fashion.
            lenient.setVisible(false);
            lenient.setCanBeConsumed(false);
            lenient.setCanBeResolved(true);
        });
    }

    default Configuration config(String suffix) {
        return config(suffix, Actions.doNothing());
    }

    default Configuration config(String suffix, Action<? super Configuration> whenCreated) {
        final ConfigurationContainer configs = getConfigurations();
        final String srcName = getSrcName();
        String name = srcName.equals(suffix) ? suffix : srcName + GUtil.toCamelCase(suffix);
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
}
