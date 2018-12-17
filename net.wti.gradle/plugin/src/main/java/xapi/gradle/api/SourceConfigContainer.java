package xapi.gradle.api;

import org.gradle.api.NonNullApi;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.internal.AbstractValidatingNamedDomainObjectContainer;
import org.gradle.api.tasks.SourceSet;
import org.gradle.internal.reflect.Instantiator;
import xapi.gradle.java.Java;

import static org.gradle.api.internal.CollectionCallbackActionDecorator.NOOP;

/**
 * An object to configure / expose the various source directories of a xapi module.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 11/15/18 @ 12:28 AM.
 */
@NonNullApi
public class SourceConfigContainer extends AbstractValidatingNamedDomainObjectContainer<SourceConfig> {

    private final Project project;

    public SourceConfigContainer(Project project, Instantiator instantiator) {
        super(SourceConfig.class, instantiator, NOOP);
        this.project = project;
    }

    @Override
    protected SourceConfig doCreate(String name) {
        final ArchiveType type = ArchiveType.coerceArchiveType(name);
        final Configuration con = project.getConfigurations().getByName(type.sourceName());
        final SourceSet source = Java.sources(project).getByName(type.sourceName());
        return new SourceConfig(project, con, type, source);
    }
}
