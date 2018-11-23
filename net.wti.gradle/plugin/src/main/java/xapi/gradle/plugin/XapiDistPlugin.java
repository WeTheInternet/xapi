package xapi.gradle.plugin;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.internal.reflect.Instantiator;
import xapi.gradle.dist.DistBuilder;

import javax.inject.Inject;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 11/4/18 @ 2:55 AM.
 */
public class XapiDistPlugin implements Plugin<Project> {

    private final Instantiator instantiator;

    @Inject
    public XapiDistPlugin(Instantiator instantiator) {
        this.instantiator = instantiator;
    }


    @Override
    public void apply(Project project) {
        final XapiExtension existing = project.getExtensions().findByType(XapiExtension.class);
        if (existing != null && !(existing instanceof XapiExtensionDist)) {
            throw new GradleException("The xapi-dist plugin must be the first xapi plugin installed; preferably " +
                "using plugins { id 'xapi-dist' } or equivalent as early as possible.");
        }

        XapiExtension config = project.getExtensions().create(XapiExtension.class, "xapi", XapiExtensionDist.class, project, instantiator);
        // It is safe to cast config to ExtensionAware
        ExtensionAware ext = (ExtensionAware) config;
        ext.getExtensions().create("dist", DistBuilder.class);

        // Apply the base plugin AFTER we've taken over the XapiExtension `xapi { }`.
        project.getPlugins().apply(XapiBasePlugin.class);
    }
}
