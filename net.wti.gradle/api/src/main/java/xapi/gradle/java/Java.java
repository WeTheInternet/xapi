package xapi.gradle.java;

import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSetContainer;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 11/19/18 @ 1:09 AM.
 */
public class Java {
    public static SourceSetContainer sources(Project p) {
        JavaPluginConvention java = p.getConvention().getPlugin(JavaPluginConvention.class);
        final SourceSetContainer sourceSets = java.getSourceSets();
        return sourceSets;
    }
}
