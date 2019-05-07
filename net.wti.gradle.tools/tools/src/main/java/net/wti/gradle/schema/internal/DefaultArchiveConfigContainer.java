package net.wti.gradle.schema.internal;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.schema.api.ArchiveConfig;
import net.wti.gradle.schema.api.PlatformConfig;
import net.wti.gradle.system.impl.DefaultRealizableNamedObjectContainer;
import org.gradle.api.internal.CollectionCallbackActionDecorator;

import java.util.concurrent.Callable;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/28/18 @ 1:47 PM.
 */
public class DefaultArchiveConfigContainer extends DefaultRealizableNamedObjectContainer<ArchiveConfig> implements
    ArchiveConfigContainerInternal {

    private final Callable<PlatformConfigInternal> platform;
    private final ProjectView view;

    private boolean withClassifier, withCoordinate, withSourceJar;

    public DefaultArchiveConfigContainer(
        Callable<PlatformConfigInternal> platform, ProjectView view
    ) {
        super(ArchiveConfig.class, view.getInstantiator(), CollectionCallbackActionDecorator.NOOP);
        this.view = view;
        this.platform = platform;
    }

    @Override
    protected ArchiveConfigInternal doCreate(String name) {
        try {
            final PlatformConfigInternal plat = platform.call();
            final PlatformConfig platRoot = plat.getRoot();
            final DefaultArchiveConfig arch = new DefaultArchiveConfig(plat, view, name);
            if (plat != platRoot) {
                arch.fixRequires(platRoot);
            }
            return arch;
        } catch (Exception e) {
            throw new UnsupportedOperationException("Could not create " + name, e);
        }
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

    @Override
    public String toString() {
        return "\nDefaultArchiveConfigContainer{" +
            "withClassifier=" + withClassifier +
            ", withCoordinate=" + withCoordinate +
            ", withSourceJar=" + withSourceJar +
            ", items=" + getAsMap() +
            "}\n";
    }
}
