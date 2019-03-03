package xapi.gradle;

import org.gradle.api.Project;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository.MetadataSources;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.internal.reflect.Instantiator;
import xapi.gradle.plugin.XapiExtension;

import java.io.File;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 11/11/18 @ 2:15 AM.
 */
public class X_Gradle {

    private X_Gradle() {}

    public static void init(Project p) {
//        p.getPlugins().apply(IdeaPlugin.class);
//        final IdeaModel idea = p.getExtensions().getByType(IdeaModel.class);

        for (ArtifactRepository repo : p.getRepositories()) {
            if ("xapiLocal".equals(repo.getName())) {
                return;
            }
        }


        File xapiLoc = p.getRootDir().getParentFile();
        if (!xapiLoc.isDirectory()) {
            xapiLoc = new File(p.getRootDir().getParentFile(), XapiExtension.EXT_NAME);
            if (!xapiLoc.isDirectory()) {
                xapiLoc = new File(p.getRootDir().getParentFile().getParentFile(), XapiExtension.EXT_NAME);
            }
        }
        if (xapiLoc.isDirectory()) {
            final File loc = xapiLoc;
            p.getRepositories().maven(repo->{
                repo.setName("xapiLocal");
                repo.setUrl(new File(loc, "repo"));
                if (!"true".equals(System.getProperty("no.metadata"))) {
                    repo.metadataSources(MetadataSources::gradleMetadata);
                }
            });
        }
        // TODO: remove the need for both of these, by priming our xapiLocal repo...
//        p.getRepositories().mavenLocal();
        p.getRepositories().jcenter();

    }

    public static XapiExtension createConfig(Project p, Instantiator instantiator) {
        final ExtensionContainer ext = p.getExtensions();
        final XapiExtension config = ext.create("xapi", XapiExtension.class, p, instantiator);
        return config;
    }

}
