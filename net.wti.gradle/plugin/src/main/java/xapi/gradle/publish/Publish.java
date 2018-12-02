package xapi.gradle.publish;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.execution.TaskExecutionGraph;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenArtifact;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository;
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
import xapi.fu.In1Out1;
import xapi.gradle.api.ArchiveType;
import xapi.gradle.api.DefaultArchiveType;
import xapi.gradle.plugin.XapiBasePlugin;
import xapi.gradle.plugin.XapiExtension;
import xapi.gradle.task.SourceJar;

import java.io.File;
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
        Callable<Boolean> required = ()->{
            final TaskExecutionGraph graph = p.getGradle().getTaskGraph();
            String pathRoot = p == p.getRootProject() ? ":" : p.getPath() + ":";
            return graph.hasTask(pathRoot + "uploadArchives")
                   // This could(should?) be tailored to fit the isBintray/isArtifactory dichotomy
                   || graph.hasTask(pathRoot + "artifactoryPublish")
                   || graph.hasTask(pathRoot  + PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME);
        };
        sign.setRequired(required);

        if (isBintray(p)) {
            // delegate to bin-tray-only implementation
            BintrayPublish.publishBintray(p);
        } else {
//            throw new UnsupportedOperationException("Only bintray is currently supported; pull requests welcome!");
        }

        // Now that we have plugins all installed, setup archives for sources, javadocs and tests.
        setupArchives(p, types);

    }

    private static void setupArchives(Project p, ArchiveType ... requestedTypes) {
        p.getPlugins().withType(XapiBasePlugin.class).all(
            plugin ->{
                final ArchiveType[] types = requestedTypes.length == 0 ? DEFAULT_TYPES : requestedTypes;
                // It's a java plugin, setup publishing for it.
                final TaskContainer tasks = p.getTasks();
                final JavaPluginConvention java = p.getConvention().getPlugin(JavaPluginConvention.class);
                final SourceSetContainer sourceSets = java.getSourceSets();


                PublishingExtension publishing = p.getExtensions().getByType(PublishingExtension.class);
                boolean rootIsXapi = "xapi".equals(p.getRootDir().getName());

                publishing.repositories( repos -> {
                        repos.maven( maven -> {
                                maven.setName(rootIsXapi ? "xapiLocal" : "buildLocal");
                                maven.setUrl(new File(p.getRootDir(), "repo"));
                            }
                        );
                        if (!rootIsXapi) {
                            File f = new File(p.getRootDir().getParentFile(), "xapi");
                            if (!f.isDirectory()) {
                                f = new File(p.getRootDir().getParentFile().getParentFile(), "xapi");
                            }
                            if (f.isDirectory()) {
                                final File local = new File(f, "repo");
                                repos.maven( maven -> {
                                        maven.setName("xapiLocal");
                                        maven.setUrl(local);
                                    }
                                );

                            }
                        }
                    }
                );
                final DefaultTask pubXapi = tasks.create("publishXapi", DefaultTask.class);
                final DefaultTask pubLocal = rootIsXapi ? null : tasks.create("publishLocal", DefaultTask.class);
                tasks.withType(PublishToMavenRepository.class).all(
                    ptml -> {
                        if (ptml.getName().contains("XapiLocal")) {
                            pubXapi.dependsOn(ptml);
                            ptml.dependsOn(BasePlugin.ASSEMBLE_TASK_NAME);
                        } else if (ptml.getName().contains("BuildLocal")) {
                            pubLocal.dependsOn(ptml);
                        }
                    ptml.doLast(t->{
                        // TODO: if these tasks are actually in the task graph,
                        // we should pre-clean the directories early,
                        // if the version is a SNAPSHOT...
                        String coords = ptml.getPublication().getGroupId() + ":"
                            + ptml.getPublication().getArtifactId() + ":"
                            + ptml.getPublication().getVersion();
                        p.getGradle().buildFinished(res->{

                            ptml.getLogger().info("Published artifact to {} -> {}", ptml.getRepository().getUrl(), coords);
                            ptml.getLogger().trace("Files copied: {}", ptml.getPublication().getArtifacts().stream()
                                .map(In1Out1.<MavenArtifact, String>unsafe(i->i.getFile().getAbsolutePath()).toFunction())
                                .collect(Collectors.joining("/n")));
                        });
                    });
                    tasks.getByName("build").finalizedBy(ptml);
                });
                publishing.getPublications().create("main", MavenPublication.class,
                    pub -> {
                        // Was trying to get gradle to not publish SNAPSHOT as timestamp'd entities,
                        // but it's hardcoded into the build logic... could consider groovy MOP to swap out the method...
//                      ((MavenPublicationInternal)pub).getMavenProjectIdentity().getVersion().set("0.5.1");
                    p.getGradle().projectsEvaluated(ready->{
                        String type = (String) p.findProperty("xapi.main.component");
                        if (type == null) {
                            type = "java";
                        }
                        pub.from(p.getComponents().getByName("java"));
                        final String baseName = ((Jar)p.getTasks().getByName("jar")).getBaseName();
                        pub.setArtifactId(baseName);
                        final Task shadowJar = p.getTasks().findByName("shadowJar");
                        if (shadowJar != null) {
                            pub.artifact(shadowJar);
                        }
                    });
                });


                XapiExtension ext = (XapiExtension) p.getExtensions().findByName("xapi");

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
