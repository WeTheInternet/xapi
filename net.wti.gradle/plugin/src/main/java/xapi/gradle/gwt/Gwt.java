package xapi.gradle.gwt;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.testing.Test;
import xapi.fu.itr.SingletonIterator;
import xapi.gradle.java.Java;
import xapi.gradle.plugin.XapiBasePlugin;
import xapi.gradle.plugin.XapiExtension;
import xapi.gradle.task.SourceJar;
import xapi.gradle.tools.Ensure;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import static xapi.gradle.tools.Ensure.projectEvaluated;

/**
 * A class to expose some generally useful gwt-related utility methods to gradle.
 *
 * This is in lieu of a proper plugin, while we are getting the build converted from maven.
 *
 * Some of these helpers will be specific to xapi's project structure,
 * while others may be reusable by others.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 11/3/18 @ 2:23 AM.
 */
public class Gwt {
    public static void maybeInstall(Project p) {
        Ensure.projectEvaluated(p, ignored->{
            File f = p.getProjectDir(), r = p.getRootDir();
            while (!f.equals(r)) {
                switch (f.getName()) {
                    case "gwt":
                        addGwtDependencies(p);
                        p.getTasks().named(JavaPlugin.TEST_TASK_NAME, t->
                            ((Test)t).setMaxHeapSize("1G")
                        );
                        return;
                }
                f = f.getParentFile();
            }
        });

    }

    private static void addGwtDependencies(Project p) {
        // Do recursive compile-scope dependency resolution,
        // adding all source jars we come across (and forcing said jars to be published).
        Set<String> seen = new LinkedHashSet<>();
        mapConfig(seen, p, p, JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME);
        mapConfig(seen, p, p, JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME, JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME);
        p.getDependencies().add(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME,
            "net.wetheinter:gwt-dev:2.8.0");
        p.getDependencies().add(JavaPlugin.TEST_RUNTIME_ONLY_CONFIGURATION_NAME,
            "net.wetheinter:gwt-dev:2.8.0");

        p.apply(config ->
            config.from(new File(p.getRootDir(), "gradle/gwt-test.gradle"))
        );
    }

    private static void mapConfig(Set<String> seen, Project into, Project from, String readFrom, String writeTo) {
        if (into == from) {
            // add own sources to test compile
            if (JavaPlugin.TEST_COMPILE_CLASSPATH_CONFIGURATION_NAME.equals(readFrom)) {
                writeDep(into, into, writeTo, "default");
            }
        }
        from.getPlugins().apply(XapiBasePlugin.class);
        into.getLogger().debug("Mapping config from {}.{} to {}.{}", from, readFrom, into, writeTo);

        Ensure.projectEvaluated(from, ignored->{
            final Configuration read = from.getConfigurations().getByName(readFrom);
            into.getLogger().debug("{}.{} dependencies {}", from, readFrom, read.getAllDependencies().toArray());
            SingletonIterator
                .singleItem(read)
                .plus(read.getExtendsFrom())
                .forAll(config ->{
//                    if (!config.isCanBeConsumed()) {
//                        return;
//                    }
                    config.getAllDependencies().all(dep->{

                        if (dep instanceof ProjectDependency) {
                            final ProjectDependency projectDep = ((ProjectDependency) dep);
                            final Project source = projectDep.getDependencyProject();
                            if (!seen.add(source.getPath())) {
                                return; //lambda-ese for continue;
                            }
                            source.getPlugins().apply(XapiBasePlugin.class);
                            projectEvaluated(source, ignore-> {

                                String targetConfig = projectDep.getTargetConfiguration();
                                if (targetConfig == null) {
                                    targetConfig = "default";
                                }
                                writeDep(into, source, writeTo, targetConfig);
                                mapConfig(seen, into, source, readFrom, writeTo);
                            });

                        }
                    });
                }
            );
            read.getAllDependencies().all(dep->{
            });
        });
    }

    private static void writeDep(Project into, Project source, String writeTo, String targetConfig) {
        XapiExtension xapi = (XapiExtension) source.getExtensions().getByName("xapi");
        xapi.publish(publish->
            publish.setSources(true)
        );
        // TODO: bother with targetConfig to get non-standard jar types w/ source
        Callable<Task> getSrc = ()-> {
            final TaskProvider<SourceJar> sourceJar = xapi.getJars().get().getSources();

            sourceJar.get();
            return source.getTasks().getByName(sourceJar.getName());
        };

        try {
            String[] items = source.getPath().split(":");
            into.getLogger().trace("Adding {} to {}.{}", items[items.length-1], into, writeTo);

            // TODO: when in dev mode, use the source set.  When in prod mode, use source jars...
            // Then, make sure the default for intellij is dev mode, so we get instantaneous change propagation

            into.getDependencies().add(writeTo, source.files(getSrc.call()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ConfigurableFileCollection findSources(Project p) {
        return findSources(p, false);
    }
    public static ConfigurableFileCollection findSources(Project p, boolean withTests) {
        final SourceSetContainer sourceSets = Java.sources(p);
        Callable<Set<File>> sources = ()-> {
            Set<File> all = new LinkedHashSet<>();
            addSource(all, sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME));
            if (withTests) {
                addSource(all, sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME));

            }
            return all;
        };
        return p.files(sources);
    }

    private static void addSource(Set<File> all, SourceSet source) {
        add(all, source.getJava().getSrcDirs());
        add(all, source.getResources().getSrcDirs());
    }

    private static void add(Set<File> all, Set<File> srcDirs) {
        for (File srcDir : srcDirs) {
            if (!srcDir.exists()) {
                continue;
            }
            if (srcDir.isDirectory()) {
                all.add(srcDir);
            } else if (srcDir.isFile()) {
                if (srcDir.getName().contains(".jar") || srcDir.getName().contains(".zip")) {
                    all.add(srcDir);
                } else {
                    throw new GradleException("Cannot add file " + srcDir + " as a classpath item");
                }
            }
        }

    }
}
