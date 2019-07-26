package xapi.gradle.meta;

import com.github.javaparser.ParseException;
import net.wti.gradle.internal.api.ProjectView;
import net.wti.manifest.ManifestPlugin;
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
import xapi.gradle.paths.AllPaths;
import xapi.gradle.task.XapiManifest;

import java.io.*;
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

    private Logger logger;

    @Override
    public void apply(Project project) {
        ProjectView view = ProjectView.fromProject(project);
        // Make sure we attach a XapiManifest task
        view.getPlugins().apply(ManifestPlugin.class);

        // Alright!  The end-goal of the meta plugin is pretty grandiose,
        // but this first version is a total hack to get something useful immediately.

        // We are only going to support some "copy and transform source" tasks,
        // for the purposes of getting an absolutely tiny classpath needed for wti recompiles.

        // We'll just run the copy sync tasks repeatedly, and re-process and source that changes.
        buildStaging(project);

        logger = view.getLogger();
    }

    protected void buildStaging(Project project) {
        ProjectView view = ProjectView.fromProject(project);

        // Alright; lets grab the runtime classpath, and collect up allll the source directories.
        view.getTasks().register("xapiStaging", Sync.class, sync-> {
            sync.setDestinationDir(new File(project.getBuildDir(), "xapiStaging"));
            final SourceSet main = view.getMainSources();
            sync.dependsOn(main.getClassesTaskName());

            sync.whenSelected(selected -> {

                LinkedHashSet<String> cp = new LinkedHashSet<>();
                addProject(cp, view);

                view.getLogger().quiet("Creating staging environment for {} w/ classpath {}", view.getPath(), cp);
                sync.doFirst(t ->
                    view.getLogger().quiet("Building meta project from {} ", cp)
                );
                for (String item : cp) {
                    if (
                        // dirty, dirty, dirty....
                        item.contains("gwt-dev") ||
                        item.endsWith("tools.jar")
                    ) {
                        continue;
                    }
                    File f = new File(item);
                    if (f.isFile()) {
                        sync.from(project.zipTree(f), spec -> {
                            spec.into("/");
                        });
                    } else if (f.isDirectory()) {
                        sync.from(f);
                    } else {
                        sync.getLogger().error("Missing directory/file {}", f);
                    }
                }
            });
        });
    }

    private void addProject(LinkedHashSet<String> cp, ProjectView view) {
        // TODO: make the source set chosen configurable, w/ a default of main if a preferred choice is missing.
        final SourceSet main = view.getMainSources();
        final In1<String> adder = cp::add;

        // add all java-esque sourceset directories to our classpath
        //noinspection ResultOfMethodCallIgnored
        mapped(main.getOutput().getClassesDirs())
            .plus(main.getOutput().getResourcesDir())
            .plus(main.getOutput().getDirs())
             // auto-create any output directories; they should always exist...
             // even if outputs don't exist now, it's feasible something else wants to generate code
             // and put something in there before this classpath/metadata is consumed
            .spy(File::mkdirs)
            .plus(
                // plus sources... maybe condense to main.getAllSources()?
                // If we want to coexist with unknown languages, we probably shouldn't use allSources.
                mapped(main.getAllJava().getSrcDirs())
                .plus(main.getResources().getSrcDirs())
                // filter out all source directories which don't actually exist
                .filter(File::exists)
            )

            .map(File::getAbsolutePath)
            .forAll(adder);

        cp.remove("");
        getClasspath(cp, view, main).forAll(adder);

    }

    protected MappedIterable<String> getClasspath(
        LinkedHashSet<String> cp,
        ProjectView view,
        SourceSet main
    ) {
        SetLike<String> results = X_Jdk.setLinkedSynchronized();
        Do blockAll = Do.NOTHING;

        for (String itemPath : cp) {
            File item = new File(itemPath);
            if (itemPath.endsWith(".jar")) {
                // If it's a jar, try to load its embedded xapi metadata
                final ForkJoinTask<?> task = ForkJoinPool.commonPool().submit(() ->
                    loadFromJar(view, results, item)
                );
                blockAll = blockAll.doAfterUnsafe(task::get);
            } else if (item.isDirectory()) {
                final ForkJoinTask<?> task = ForkJoinPool.commonPool().submit(() ->
                    loadFromDir(view, results, item)
                );
                blockAll = blockAll.doAfterUnsafe(task::get);
            } else {
                if ("paths.xapi".equals(itemPath)) {
                    final ForkJoinTask<?> task = ForkJoinPool.commonPool().submit(() -> {
                            loadXapi(itemPath, new FileInputStream(item), results);
                            return null; // make this lambda look like Callable so we can freely throw checked exception
                        }
                    );
                    blockAll = blockAll.doAfterUnsafe(task::get);
                } else {
                    // a plain file.  take it, and go...
                    results.add(item.getAbsolutePath());
                }
            }
        }
        blockAll.done();
        return results;
    }

    private boolean loadFromDir(ProjectView view, SetLike<String> results, File item) throws IOException {

        String path = item.getAbsolutePath().replace('\\', '/');
        XapiManifest manifest = XapiManifest.fromView(view).get();
        String buildSeg = manifest.getOutputDirName().get(); // hm.  this needs to not-be configurable per-project.
        // it'll need to be configured per-build, via xapi-root plugin

        // a little paranoid; we _should_ be able to depend on a leading / character,
        // but we are resolving end user values, so we might as well be a little more lenient
        if (buildSeg.startsWith("/")) {
            buildSeg = buildSeg.substring(1);
        }
        path = path.split(buildSeg)[0] + buildSeg + "/" + xapi.gradle.task.XapiManifest.DEFAULT_OUTPUT_DIR + "/META-INF/xapi";
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

    private boolean loadFromJar(ProjectView view, SetLike<String> results, File item) throws IOException {
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
