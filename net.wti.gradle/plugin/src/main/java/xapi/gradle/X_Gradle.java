package xapi.gradle;

import net.wti.gradle.system.plugin.XapiBasePlugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.internal.reflect.Instantiator;
import xapi.gradle.plugin.XapiExtension;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 11/11/18 @ 2:15 AM.
 */
public class X_Gradle {

    private X_Gradle() {}

    public static void init(Project p) {
//        final IdeaModel idea = p.getPlugins().apply(IdeaPlugin.class).getModel();
        for (ArtifactRepository repo : p.getRepositories()) {
            if ("xapiLocal".equals(repo.getName())) {
                return;
            }
        }
        p.getPlugins().apply(XapiBasePlugin.class);
    }

    public static XapiExtension createConfig(Project p, Instantiator instantiator) {
        final ExtensionContainer ext = p.getExtensions();
        final XapiExtension config = ext.create("xapi", XapiExtension.class, p, instantiator);
        return config;
    }

}
