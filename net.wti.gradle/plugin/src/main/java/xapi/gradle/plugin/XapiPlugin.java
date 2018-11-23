package xapi.gradle.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * The opinionated counterpart of the XapiBasePlugin.
 *
 * This plugin is expected to be used by poly-sourceset modules,
 * which assembles (and publishes) a group of related archives.
 *
 * We take care of and expose the mappings between sourcesets, jars, classpaths and
 * transitive dependency graphs.
 *
 * So, for each sourceSet which exists,
 * either by presence of a directory in src/
 * or by configuration in gradle dsl (example showing default values):
xapi {
    gwt {
        src = 'src/gwt/java'
        test = 'src/gwtTest/java'
    }
    api {
        src = 'src/api/java'
        test = 'src/apiTest/java'
    }
}
 * or, simply:
 *
 xapi { gwt ; api ; stub ; spi }
 *
 * which will create the source routes and associated tasks for you.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 11/14/18 @ 11:37 PM.
 */
public class XapiPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {

        project.getPlugins().apply(XapiBasePlugin.class);



    }
}
