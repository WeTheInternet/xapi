package net.wti.gradle.schema.internal;

import net.wti.gradle.internal.require.api.ArchiveGraph;
import net.wti.gradle.internal.require.api.PlatformGraph;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.tasks.SourceSet;

/**
 * This is a context object we glue onto all sourcesets we create
 * (and onto the ext objects of any Dependency objects we create,
 * so we can locate them from a configuration's dependency graph).
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 12/29/18 @ 4:17 AM.
 */
public class SourceMeta {

    public static final String EXT_NAME = "xapiSrc";
    private final ArchiveGraph archive;
    private final PlatformGraph platform;
    private final SourceSet src;

    public SourceMeta(PlatformGraph platform, ArchiveGraph archive, SourceSet src) {
        this.archive = archive;
        this.platform = platform;
        this.src = src;
    }

    public ArchiveGraph getArchive() {
        return archive;
    }

    public PlatformGraph getPlatform() {
        return platform;
    }

    public SourceSet getSrc() {
        return src;
    }

    /**
     * Create a dependency appropriate for insertion into the provided {@link ArchiveGraph}
     *
     * If the target
     *
     * @param into The archive you are creating a dependency for.
     * @return a suitable Dependency object (use into.getView().getDependencies().create()),
     * which has an extension property, {@link #EXT_NAME} (xapiSrc) pointing to this SourceMeta object.
     */
    public Dependency depend(ArchiveGraph into) {
        DependencyHandler deps = into.getView().getDependencies();

        final FileCollection cp = isRuntime(into) ?
            src.getRuntimeClasspath() :
            src.getCompileClasspath().plus(
                src.getAllSource().getSourceDirectories()
            );
        final Dependency dep = deps.create(cp);
        ((ExtensionAware)dep).getExtensions().add(EXT_NAME, this);
        return dep;
    }

    public boolean isRuntime(ArchiveGraph into) {
        return into.getSrcName().toLowerCase().contains("runtime");
    }

}
