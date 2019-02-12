package net.wti.gradle.schema.internal;

import net.wti.gradle.schema.api.ArchiveConfig;
import net.wti.gradle.schema.api.ArchiveConfigContainer;
import org.gradle.api.internal.AbstractNamedDomainObjectContainer;
import org.gradle.api.internal.CollectionCallbackActionDecorator;
import org.gradle.internal.reflect.Instantiator;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/28/18 @ 1:47 PM.
 */
public class DefaultArchiveConfigContainer extends AbstractNamedDomainObjectContainer<ArchiveConfig> implements
    ArchiveConfigContainerInternal {

    private boolean withClassifier, withCoordinate, withSourceJar;

    public DefaultArchiveConfigContainer(
        Instantiator instantiator
    ) {
        super(ArchiveConfig.class, instantiator, CollectionCallbackActionDecorator.NOOP);
    }

    @Override
    protected ArchiveConfigInternal doCreate(String name) {
        return new DefaultArchiveConfig(name);
    }

    @Override
    public ArchiveConfigInternal maybeCreate(String name) {
        return (ArchiveConfigInternal) super.maybeCreate(name);
    }

    @Override
    public boolean isWithClassifier() {
        return withClassifier;
    }

    @Override
    public void setWithClassifier(boolean withClassifier) {
        this.withClassifier = withClassifier;
    }

    @Override
    public boolean isWithCoordinate() {
        return withCoordinate;
    }

    @Override
    public void setWithCoordinate(boolean withCoordinate) {
        this.withCoordinate = withCoordinate;
    }

    @Override
    public boolean isWithSourceJar() {
        return withSourceJar;
    }

    @Override
    public void setWithSourceJar(boolean withSourceJar) {
        this.withSourceJar = withSourceJar;
    }
}
