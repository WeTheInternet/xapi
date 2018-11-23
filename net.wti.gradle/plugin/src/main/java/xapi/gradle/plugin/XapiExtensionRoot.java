package xapi.gradle.plugin;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.internal.reflect.Instantiator;
import xapi.fu.In1Out1;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 11/22/18 @ 10:29 PM.
 */
public class XapiExtensionRoot extends XapiExtension {

    private final ListProperty<ModuleDependency> localCache;
    private final Property<Boolean> strictCaching;
    private final In1Out1<String, Dependency> resolver;

    public XapiExtensionRoot(Project project, Instantiator instantiator) {
        super(project, instantiator);
        localCache = project.getObjects().listProperty(ModuleDependency.class);
        strictCaching = project.getObjects().property(Boolean.class);
        resolver = project.getDependencies()::create;
    }

    public ListProperty<ModuleDependency> getLocalCache() {
        return localCache;
    }

    protected boolean isStrict() {
        return strictCaching.isPresent() ? strictCaching.get() : false;
    }

    public void preload(String notation) {
        final Dependency dependency = resolver.io(notation);
        if (!(dependency instanceof ModuleDependency)) {
            throw new GradleException("Invalid notation " + notation +"; only module:identifiers:expected");
        }
        getLocalCache().add((ModuleDependency) dependency);
    }
}
