package xapi.gradle.java;

import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSetContainer;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 11/19/18 @ 1:09 AM.
 */
public class Java {
    public static SourceSetContainer sources(Project p) {
        JavaPluginConvention java;
        try {
            java = p.getConvention().getPlugin(JavaPluginConvention.class);
        } catch (IllegalStateException e) {
            if (!e.getMessage().contains("find any convention")) {
                throw e;
            }
            p.getPlugins().apply("java-base");
            java = p.getConvention().getPlugin(JavaPluginConvention.class);
        }
        final SourceSetContainer sourceSets = java.getSourceSets();
        return sourceSets;
    }
}
