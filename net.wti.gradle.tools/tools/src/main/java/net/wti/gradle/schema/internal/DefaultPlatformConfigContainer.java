package net.wti.gradle.schema.internal;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.schema.api.ArchiveConfigContainer;
import net.wti.gradle.schema.api.PlatformConfig;
import net.wti.gradle.system.impl.DefaultRealizableNamedObjectContainer;
import org.gradle.api.internal.CollectionCallbackActionDecorator;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/28/18 @ 2:34 PM.
 */
public class DefaultPlatformConfigContainer extends DefaultRealizableNamedObjectContainer<PlatformConfig> implements
    PlatformConfigContainerInternal {

    private final ArchiveConfigContainer schemaArchives;
    private final ProjectView view;

    public DefaultPlatformConfigContainer(ProjectView pv, ArchiveConfigContainer schemaArchives) {
        super(PlatformConfig.class, pv.getInstantiator(), CollectionCallbackActionDecorator.NOOP);
        this.view = pv;
        this.schemaArchives = schemaArchives;
    }

    @Override
    protected PlatformConfigInternal doCreate(String name) {
        return new DefaultPlatformConfig(name, this, view, schemaArchives);
    }

    @Override
    public PlatformConfigInternal maybeCreate(String name) {
        return (PlatformConfigInternal) super.maybeCreate(name);
    }

    @Override
    public String toString() {
        return "DefaultPlatformConfigContainer{" +
            getAsMap() +
            "} ";
    }

    @Override
    public ProjectView getView() {
        return view;
    }

    @Override
    public PlatformConfigInternal findByName(String name) {
        return (PlatformConfigInternal) super.findByName(name);
    }
}
