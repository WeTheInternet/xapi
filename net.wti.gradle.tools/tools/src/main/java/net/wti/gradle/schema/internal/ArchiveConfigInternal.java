package net.wti.gradle.schema.internal;

import net.wti.gradle.internal.require.api.ArchiveGraph;
import net.wti.gradle.internal.require.api.PlatformGraph;
import net.wti.gradle.internal.require.api.ProjectGraph;
import net.wti.gradle.schema.api.ArchiveConfig;
import net.wti.gradle.schema.api.PlatformConfig;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/29/18 @ 11:06 PM.
 */
public interface ArchiveConfigInternal extends ArchiveConfig {

    default ArchiveGraph findGraph(ProjectGraph graph) {
        final PlatformConfig platform = getPlatform();
        final PlatformGraph platformGraph = graph.platform(platform.getName());
        return platformGraph.archive(getName());
    }

    void fixRequires(PlatformConfig platConfig);

    void baseOn(ArchiveConfig rooted);
}
