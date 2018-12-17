package xapi.gradle.publish;

import net.wti.gradle.PublishXapi;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.execution.TaskExecutionGraph;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.publish.plugins.PublishingPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.plugins.signing.SigningExtension;
import org.gradle.plugins.signing.SigningPlugin;
import org.gradle.util.GFileUtils;
import xapi.fu.Lazy;
import xapi.fu.Mutable;
import xapi.gradle.api.ArchiveType;
import xapi.gradle.api.DefaultArchiveType;
import xapi.gradle.plugin.XapiBasePlugin;
import xapi.gradle.plugin.XapiExtension;
import xapi.gradle.task.SourceJar;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * A utility class for installing project publishing.
 *
 * In the future we should detect if artifactory/bintray/etc is being used;
 * for now, this is hardwired to the xapi build (bintray / oss), but we'll make it
 * a generic standalone plugin when moving to support wti (artifactory / private).
 *
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 11/4/18 @ 12:29 AM.
 */
public class Publish {

    private static final ArchiveType[] DEFAULT_TYPES = {
        DefaultArchiveType.MAIN,
        DefaultArchiveType.SOURCE,
        DefaultArchiveType.JAVADOC,
        // Test archives will be published with a modified artifactId, not a classifier.
        // This allows their transitive dependency graph to persist as a standalone pom file.
        DefaultArchiveType.TEST,
        DefaultArchiveType.TEST_SOURCE
    };

    public static void addPublishing(Project p, ArchiveType ... types) {
        if ("true".equals(p.getExtensions().findByName("xapi-publish-setup"))) {
            return;
        }
        p.getExtensions().add("xapi-publish-setup", "true");

        p.getPlugins().apply(MavenPublishPlugin.class);
        p.getPlugins().apply(SigningPlugin.class);
        // This cast is safer than extensions.getByType; extensions are only unique by name, not type.
        SigningExtension sign = (SigningExtension) p.getExtensions().getByName("signing");

        // TODO: get xapi-fu in here, so we can use Lazy<Boolean>
        Lazy<Boolean> required = Lazy.deferred1(()->{
            final TaskExecutionGraph graph = p.getGradle().getTaskGraph();
            final Mutable<Boolean> ready = new Mutable<>(false);
            graph.whenReady(g->{
                ready.in(true);
            });
            if (ready.out1()) {
                // graph is available to be read...
                String pathRoot = p == p.getRootProject() ? ":" : p.getPath() + ":";
                return graph.hasTask(pathRoot + "uploadArchives")
                       // This could(should?) be tailored to fit the isBintray/isArtifactory dichotomy
                       || graph.hasTask(pathRoot + "artifactoryPublish")
                       || graph.hasTask(pathRoot + "publishXapi")
                       || graph.hasTask(pathRoot + "pX")
                       || graph.hasTask(pathRoot  + PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME);
            } else {
                // We have to guess from the startParameters.
                final List<String> requested = p.getGradle().getStartParameter().getTaskNames();
                for (String r : requested) {
                    if (r.toLowerCase().contains("publish") || "pX".equals(r) || r.contains("upload")) {
                        return true;
                    }
                }
                return false;
            }
        });
        sign.setRequired(required.asCallable());

        if (isBintray(p)) {
            // delegate to bin-tray-only implementation
            BintrayPublish.publishBintray(p);
        } else {
//            throw new UnsupportedOperationException("Only bintray is currently supported; pull requests welcome!");
        }

        // Now that we have plugins all installed, setup archives for sources, javadocs and tests.
        setupArchives(p, required, types);

    }

    private static void setupArchives(
        Project p,
        Lazy<Boolean> required,
        ArchiveType... requestedTypes
    ) {
        p.getPlugins().withType(XapiBasePlugin.class).all(
            plugin ->{
//                final ArchiveType[] types = requestedTypes.length == 0 ? DEFAULT_TYPES : requestedTypes;
                // It's a java plugin, setup publishing for it.
                final TaskContainer tasks = p.getTasks();
                final JavaPluginConvention java = p.getConvention().getPlugin(JavaPluginConvention.class);
                final SourceSetContainer sourceSets = java.getSourceSets();

                XapiExtension ext = (XapiExtension) p.getExtensions().getByName("xapi");
                final MavenPublication mainPub = ext.getMainPublication();

                TaskProvider<SourceJar> sourceJar = ext.getJars().get().getSources();

                // TODO: only realize the sourceJar task if publishing is actually occurring.
                p.getArtifacts().add("archives", sourceJar.get());

                if ("true".equals(p.findProperty("xapi.release"))) {

                    Javadoc javadoc = (Javadoc) tasks.getByName("javadoc");
                    File javadocOpts = new File(p.getBuildDir(), "javadoc.opts");
                    final TaskProvider<Task> jdoc = tasks.named(JavaPlugin.JAVADOC_TASK_NAME, task -> {
                        final Set<File> srcDirs = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).getAllJava()
                            .getSrcDirs();
                        task.doFirst(t -> {
                            final String sources = srcDirs.stream()
                                .filter(File::exists)
                                .map(File::getAbsolutePath).collect(Collectors.joining(
                                File.pathSeparator));
                            GFileUtils.writeFile("-Xdoclint:none\n" +
                                "-sourcepath " + sources, javadocOpts);

                        });
                        Javadoc doc = (Javadoc) task;
                        doc.exclude("**/google/gwt/**");
                        srcDirs
                            .stream()
                            .filter(File::exists)
                            .forEach(doc.getInputs()::dir);
                        doc.getOptions().quiet();
                        doc.getOptions().source("1.8");

                        doc.getOptions().optionFiles(javadocOpts);

                    });
                    final Jar javadocJar = tasks.create("javadocJar", Jar.class, jar -> {
                        jar.dependsOn(jdoc); // TODO: test and see if we can delete this...
                        jar.setClassifier("javadoc");
                        jar.from(javadoc.getDestinationDir());
                    });
                    p.getArtifacts().add("archives", javadocJar);
                }
            }
        );
    }

    private static boolean isBintray(Project p) {
        try {
            return !p.getPlugins()
                .withType(com.jfrog.bintray.gradle.BintrayPlugin.class)
                .isEmpty()
            ||
                !p.getRootProject()
                .getPlugins()
                .withType(com.jfrog.bintray.gradle.BintrayPlugin.class)
                .isEmpty();
        } catch (NoClassDefFoundError failed) {
            return false;
        }
    }

    private static boolean isArtifactory(Project p) {
        try {
            return !p.getPlugins()
                .withType(org.jfrog.gradle.plugin.artifactory.ArtifactoryPlugin.class)
                .isEmpty()
            ||
                !p.getRootProject()
                .getPlugins()
                .withType(org.jfrog.gradle.plugin.artifactory.ArtifactoryPlugin.class)
                .isEmpty();
        } catch (NoClassDefFoundError failed) {
            return false;
        }
    }

}
