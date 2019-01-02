package net.wti.gradle.schema.internal;

import net.wti.gradle.schema.api.ArchiveConfig;
import org.gradle.api.ProjectView;
import org.gradle.api.artifacts.Configuration;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/29/18 @ 11:06 PM.
 */
public interface ArchiveConfigInternal extends ArchiveConfig {

    Configuration getTransitive(ProjectView forProject);
    Configuration getIntransitive(ProjectView forProject);
    Configuration getImplementation(ProjectView forProject);
    Configuration getRuntime(ProjectView forProject);

    /*
    attribute-wired configurations:
     * transitive, intransitive, implementation and runtime.
     *
     * In the lingo of gradle's java-library plugin,
     * transitive = apiElements
     * intransitive = compileOnly
     * implementation = implementation
     * runtime = runtimeElements
     *
     * These are the configurations you would reference when depending on another project;
     * see https://docs.gradle.org/current/userguide/java_library_plugin.html for more info.
     *
    */


}
