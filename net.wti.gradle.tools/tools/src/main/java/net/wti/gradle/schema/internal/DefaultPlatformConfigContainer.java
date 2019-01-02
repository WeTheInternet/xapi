package net.wti.gradle.schema.internal;

import net.wti.gradle.schema.api.ArchiveConfigContainer;
import net.wti.gradle.schema.api.PlatformConfig;
import net.wti.gradle.schema.api.PlatformConfigContainer;
import org.gradle.api.internal.AbstractNamedDomainObjectContainer;
import org.gradle.api.internal.CollectionCallbackActionDecorator;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.internal.reflect.Instantiator;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/28/18 @ 2:34 PM.
 */
public class DefaultPlatformConfigContainer extends AbstractNamedDomainObjectContainer<PlatformConfig> implements
    PlatformConfigContainerInternal {

    private final ArchiveConfigContainer schemaArchives;

    public DefaultPlatformConfigContainer(Instantiator instantiator, ArchiveConfigContainer schemaArchives) {
        super(PlatformConfig.class, instantiator, CollectionCallbackActionDecorator.NOOP);
        this.schemaArchives = schemaArchives;
    }

    @Override
    protected PlatformConfigInternal doCreate(String name) {
        return new DefaultPlatformConfig(name, this, getInstantiator(), schemaArchives);
    }

    @Override
    public PlatformConfigInternal maybeCreate(String name) {
        return (PlatformConfigInternal) super.maybeCreate(name);
    }
}
