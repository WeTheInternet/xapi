package xapi.gradle.meta;

import com.github.javaparser.ParseException;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.Sync;
import xapi.fu.Do;
import xapi.fu.In1;
import xapi.fu.data.SetLike;
import xapi.fu.itr.MappedIterable;
import xapi.fu.java.X_Jdk;
import xapi.gradle.java.Java;
import xapi.gradle.paths.AllPaths;
import xapi.gradle.tools.Ensure;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import static xapi.fu.itr.MappedIterable.mapped;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 11/19/18 @ 1:05 AM.
 */
public class MetaPlugin implements Plugin<Project> {

    private DirectoryProperty buildDir;
    private Logger logger;

    @Override
    public void apply(Project project) {
        // Alright!  The end-goal of the meta plugin is pretty grandiose,
        // but this first version is a total hack to get something useful immediately.

        // We are only going to support some "copy and transform source" tasks,
        // for the purposes of getting an absolutely tiny classpath needed for wti recompiles.

        // We'll just run the copy sync tasks repeatedly, and re-process and source that changes.
        buildStaging(project);

        buildDir = project.getLayout().getBuildDirectory();
        logger = project.getLogger();
    }

    protected void buildStaging(Project project) {

        // Alright; lets grab the runtime classpath, and collect up allll the source directories.
        Ensure.projectEvaluated(project, p->{

            LinkedHashSet<String> cp = new LinkedHashSet<>();
            final SourceSet sources = addProject(cp, project);
            cp.remove("");

            project.getTasks().register("xapiStaging", Sync.class, sync->{
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

        mapped(main.getOutput().getClassesDirs())
            .plus(main.getOutput().getResourcesDir())
            .plus(main.getOutput().getDirs())
            .plus(main.getAllJava().getSrcDirs())
            .plus(main.getResources().getSrcDirs())
            .filter(File::exists)
            .map(File::getAbsolutePath)
            .forAll(adder);

        getClasspath(main).forAll(adder);

        cp.remove("");
        return main;
    }

    protected MappedIterable<String> getClasspath(SourceSet main) {
        SetLike<String> results = X_Jdk.setLinkedSynchronized();
        Do blockAll = Do.NOTHING;
        for (File item : main.getCompileClasspath()) {
            if (item.getName().endsWith(".jar")) {
                // If it's a jar, try to load it's embedded xapi metadata
                final ForkJoinTask<?> task = ForkJoinPool.commonPool().submit(() ->
                    loadFromJar(results, item)
                );
                blockAll = blockAll.doAfterUnsafe(task::get);
            } else if (item.isDirectory()) {
                final ForkJoinTask<?> task = ForkJoinPool.commonPool().submit(() ->
                    loadFromDir(results, item)
                );
                blockAll = blockAll.doAfterUnsafe(task::get);
            } else {
                results.add(item.getAbsolutePath());
            }
        }
        blockAll.done();
        return results;
    }

    private boolean loadFromDir(SetLike<String> results, File item) throws IOException {

        String path = item.getAbsolutePath().replace('\\', '/');
        final String buildSeg = buildDir.getAsFile().get().getName();
        path = path.split(buildSeg)[0] + buildSeg + "/xapi-paths/META-INF/xapi";
        File main = new File(path, "paths.xapi");
        if (!main.exists()) {
            results.add(item.getAbsolutePath());
            return false;
        }
        try (
            FileInputStream in = new FileInputStream(main)
        ) {
            loadXapi(main.getAbsolutePath(), in, results);
        }
        return true;
    }

    private void loadXapi(String loc, InputStream in, SetLike<String> results) {
        try {
            AllPaths paths = AllPaths.deserialize(loc, in);
            results.addNow(paths.getAllFiles(true).map(File::getAbsolutePath));
        } catch (ParseException e) {
            logger.error("Invalid xapi source at {}", loc, e);
            throw new GradleException("Invalid xapi source at " + loc);
        }
    }

    private boolean loadFromJar(SetLike<String> results, File item) throws IOException {
        if (!item.isFile()) {
            return false;
        }
        try (
            JarFile jar = new JarFile(item)
        ) {
            final ZipEntry entry = jar.getEntry("META-INF/xapi/paths.xapi");
            if (entry != null) {
                loadXapi(entry.toString(), jar.getInputStream(entry), results);
            }
        }
        return true;
    }
}
