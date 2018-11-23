package xapi.gradle.meta;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.Sync;
import xapi.fu.In1;
import xapi.fu.itr.SizedIterable;
import xapi.gradle.java.Java;
import xapi.gradle.tools.Ensure;

import java.io.File;
import java.util.LinkedHashSet;

import static xapi.fu.itr.ArrayIterable.iterate;
import static xapi.fu.itr.MappedIterable.mapped;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 11/19/18 @ 1:05 AM.
 */
public class MetaPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        // Alright!  The end-goal of the meta plugin is pretty grandiose,
        // but this first version is a total hack to get something useful immediately.

        // We are only going to support some "copy and transform source" tasks,
        // for the purposes of getting an absolutely tiny classpath needed for wti recompiles.

        // We'll just run the copy sync tasks repeatedly, and re-process and source that changes.
        buildStaging(project);

    }

    protected void buildStaging(Project project) {

        // Alright; lets grab the runtime classpath, and collect up allll the source directories.
        Ensure.projectEvaluated(project, p->{

            LinkedHashSet<String> cp = new LinkedHashSet<>();
            final SourceSet sources = addProject(cp, project);
            cp.remove("");

            project.getTasks().register("xapiMeta", Sync.class, sync->{
                project.getLogger().quiet("Building meta project from " + cp);
                sync.doFirst(t->
                    project.getLogger().quiet("Building meta project from " + cp)
                );
                sync.setDestinationDir(new File(project.getBuildDir(), "xapiStaging"));
                for (String item : cp) {
                    if (
                        item.contains("gwt-dev") ||
                        item.endsWith("tools.jar")
                    ) {
                        continue;
                    }
                    File f = new File(item);
                    if (f.isFile()) {
                        sync.from(project.zipTree(f), spec->{
                            spec.into("/");
                        });
                    } else if (f.isDirectory()) {
                        sync.from(cp);
                    } else {
                        sync.getLogger().error("Missing directory/file {}", f);
                    }
                }


                sync.dependsOn(sources.getClassesTaskName());
            });
        });

    }

    private SourceSet addProject(LinkedHashSet<String> cp, Project project) {
        // TODO: make the source set chosen configurable, w/ a default of main if a preferred choice is missing.
        final SourceSet main = Java.sources(project).getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        final In1<String> adder = cp::add;

        iterate(main.getOutput().getDirs().getAsPath().split(File.pathSeparator))
            .forAll(adder);

        mapped(main.getAllJava().getSrcDirs())
            .plus(main.getResources().getSrcDirs())
            .map(File::getAbsolutePath)
            .forAll(adder);

        getClasspath(main).forAll(adder);

        return main;
    }

    protected SizedIterable<String> getClasspath(SourceSet main) {
        final String cp = main.getCompileClasspath().getAsPath();
        return iterate(cp.split(File.pathSeparator));
    }
}
