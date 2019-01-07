package net.wti.gradle.internal.require.api;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.require.api.ArchiveRequest.ArchiveRequestType;
import net.wti.gradle.require.api.DependencyKey;
import net.wti.gradle.schema.internal.ArchiveConfigInternal;
import net.wti.gradle.schema.internal.SourceMeta;
import org.gradle.api.Action;
import org.gradle.api.Named;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.internal.Actions;
import org.gradle.util.GUtil;

import java.io.File;
import java.util.Map;
import java.util.Set;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 1/2/19 @ 4:23 AM.
 */
public
interface ArchiveGraph extends Named {
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

    default Configuration configAssembled() {
        return config("Assembled", assembled -> {
            assembled.extendsFrom(configTransitive());
        });
    }

    default Configuration configTransitive() {
        return config("Transitive", transitive-> {
            transitive.extendsFrom(platform().configGlobal());
            // This configuration is only for declaring dependencies, not resolving them.
            transitive.setVisible(false);
            transitive.setCanBeConsumed(false);
            transitive.setCanBeConsumed(false);
            if (srcExists()) {
                final Configuration api = getView().getConfigurations().findByName(getSrcName() + "Api");
                if (api == null) {
                    config("Compile").extendsFrom(transitive);
                } else {
                    config("Api").extendsFrom(transitive);
                }
            }
        });
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

    default Configuration config(String suffix) {
        return config(suffix, Actions.doNothing());
    }
    default Configuration config(String suffix, Action<? super Configuration> whenCreated) {
        final ConfigurationContainer configs = getConfigurations();
        String name = getSrcName() + suffix;
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
