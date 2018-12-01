package xapi.gradle.task;

import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import xapi.gradle.api.DefaultArchiveType;

/**
 * A jar containing sources for the main publication of a given module.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 11/4/18 @ 11:58 PM.
 */
public class SourceJar extends AbstractXapiJar {

    public SourceJar() {
        setArchiveType(DefaultArchiveType.SOURCE);
        setClassifier("sources");
        final JavaPluginConvention sources = getProject().getConvention().getPlugin(JavaPluginConvention.class);
        final SourceSet main = sources.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        from(main.getAllJava().getSrcDirs());
    }
}
