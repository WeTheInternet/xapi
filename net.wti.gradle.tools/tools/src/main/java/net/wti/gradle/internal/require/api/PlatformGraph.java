package net.wti.gradle.internal.require.api;

import net.wti.gradle.internal.api.HasWork;
import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.schema.internal.PlatformConfigInternal;
import net.wti.gradle.schema.internal.SourceMeta;
import net.wti.gradle.system.api.RealizableNamedObjectContainer;
import org.gradle.api.Action;
import org.gradle.api.Named;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.SourceSetContainer;

import java.util.Set;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 1/2/19 @ 4:23 AM.
 */
public interface PlatformGraph extends Named, HasWork {
    ProjectGraph project();

    PlatformGraph parent();

    ArchiveGraph archive(Object name);

    RealizableNamedObjectContainer<ArchiveGraph> archives();

    NamedDomainObjectProvider<ArchiveGraph> getOrRegister(String archive);

    void realizedArchives(Action<? super ArchiveGraph> action);

    Set<ArchiveGraph> realize();

    Set<ArchiveGraph> realizedArchives();

    Set<ArchiveRequest> incoming();

    Set<ArchiveRequest> outgoing();

    default boolean isSelectable() {
        return project().isSelectable(this);
    }

    default boolean matches(String requested) {
        if (requested.equals(getName())) {
            return true;
        }
        final PlatformGraph parent = parent();
        if (parent == null) {
            return false;
        }
        return parent.matches(requested);
    }

    default ProjectView getView() {
        return project().project();
    }

    default Configuration configGlobal() {
        return getView().getConfigurations().maybeCreate(getName());
    }

    PlatformConfigInternal config();

    void setParent(PlatformGraph parent);

    SourceMeta sourceFor(SourceSetContainer srcs, ArchiveGraph archive);

    default String getPath() {
        return project().getPath() + ":" + getName();
    }

    default String asGroup(String group) {
        // hm... we _may_ want platform in the group _and_ the artifactId.
        // It _must_ be in the artifact id for default jar naming conventions (and unique project names) to play nicely,
        // but it can be nice to actually have all of a given platform in a single directory, rather than a big, flat repo dir.
//        final String platId = getName();
//        if ("main".equals(platId)) {
            return group;
//        }
//        return group + "." + platId;
    }

    default String getGroup() {
        return asGroup(project().getGroup());
    }

    default ArchiveGraph getMainModule() {
        return archive(config().getMainModuleName());
    }

    default String getVersion() {
        return project().getVersion();
    }
}
