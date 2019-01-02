package net.wti.gradle.schema.internal;

import net.wti.gradle.internal.api.ProjectView;
import org.gradle.api.Named;

/**
 * A container of contextual objects scoped-to-a-single-sourceSet/buildUnit.
 *
 * Contains a {@link ProjectView}, a {@link PlatformConfigContainerInternal}
 * and an {@link ArchiveConfigContainerInternal}.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 12/31/18 @ 1:11 AM.
 */
public class BuildUnitContext implements Named {

    private final ProjectView project;
    private final PlatformConfigContainerInternal platforms;
    private final ArchiveConfigContainerInternal archives;
    private final String name;

    public BuildUnitContext(
        ProjectView project,
        PlatformConfigContainerInternal platforms,
        ArchiveConfigContainerInternal archives,
        String name
    ) {
        this.project = project;
        this.platforms = platforms;
        this.archives = archives;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
