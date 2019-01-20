package net.wti.gradle.internal.require.api;

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
public interface PlatformGraph extends Named {
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
}
