package net.wti.gradle.schema.internal;

import net.wti.gradle.schema.api.ArchiveConfig;
import net.wti.gradle.schema.api.PlatformConfig;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
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
    private final ArchiveConfig archive;
    private final PlatformConfig platform;
    private final SourceSet src;
    private final String srcName;

    public SourceMeta(PlatformConfig platform, ArchiveConfig archive, SourceSet src) {
        this.archive = archive;
        this.platform = platform;
        this.src = src;
        this.srcName = platform.sourceName(archive);
    }

    public ArchiveConfig getArchive() {
        return archive;
    }

    public PlatformConfig getPlatform() {
        return platform;
    }

    public SourceSet getSrc() {
        return src;
    }

    public Dependency depend(DependencyHandler deps) {
        final Dependency dep = deps.create(
            src.getRuntimeClasspath()
        );
        ((ExtensionAware)dep).getExtensions().add(EXT_NAME, this);
        return dep;
    }

    public String getApiConfigurationName() {
        return srcName + "Transitive";
    }

    public String getImplementationConfigurationName() {
        return srcName + "Impl";
    }
}
