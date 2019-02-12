package net.wti.gradle;

import groovy.util.Node;
import net.wti.gradle.publish.task.XapiPublish;
import net.wti.gradle.system.service.GradleService;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.SelfResolvingDependency;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository.MetadataSources;
import org.gradle.api.component.SoftwareComponentContainer;
import org.gradle.api.publish.PublicationArtifact;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.jvm.tasks.Jar;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

import java.io.File;
import java.util.stream.Collectors;

@Deprecated
public class PublishXapi {

    public static final String MAIN_PUBLICATION = "xapiMain";

    public static Task getPublishXapiTask(Project project) {
        return GradleService.buildOnce(project, XapiPublish.LIFECYCLE_TASK, p->{
                addPublishXapiTask(project);
            return project.getTasks().getByName(XapiPublish.LIFECYCLE_TASK);
        });
    }

    public static MavenPublication addPublishXapiTask(Project p) {
        return GradleService.buildOnce(p, MAIN_PUBLICATION, PublishXapi::createPublishXapiTask);
    }

    private static MavenPublication createPublishXapiTask(Project p) {
            Object home = p.getExtensions().findByName("xapi.home");
            if (home == null) {
                File f = new File(p.getRootDir().getParentFile(), "xapi");
                while (f.getParentFile() != null) {
                    final File candidate = new File(f.getParentFile(), "xapi");
                    if (candidate.isDirectory()) {
                        f = candidate;
                        break;
                    } else {
                        f = f.getParentFile();
                    }
                }
                if (!f.isDirectory()) {
                    f = new File(p.getRootDir().getParentFile().getParentFile(), "xapi");
                }
                if (f.isDirectory()) {
                    final File local = new File(f, "repo");
                    home = local.getAbsolutePath();
                }
            } else {
                File test = new File(String.valueOf(home), "repo");
                if (test.isDirectory()) {
                    home = test;
                }
            }
            PublishingExtension publishing = p.getExtensions().getByType(PublishingExtension.class);

            File xapiHome = new File(String.valueOf(home));
            publishing.repositories( repos -> {
                    repos.maven( maven -> {
                            maven.setName("xapiLocal");
                            maven.setUrl(xapiHome);
                            maven.metadataSources(MetadataSources::gradleMetadata);
                        }
                    );
                }
            );
            TaskContainer tasks = p.getTasks() ;
            final DefaultTask pubXapi = tasks.create(XapiPublish.LIFECYCLE_TASK, DefaultTask.class);
            pubXapi.setGroup("Publishing");
            pubXapi.setDescription("Publish all artifacts destined for the XapiLocal repo");

            // we'll eagerly realize if command line had xapiPublish or pX as explicit tasks.
            // not being able to query-then-mutate the task graph kinda sucks,
            // but it should at least make race conditions less murderous.
            boolean eager = p.getGradle().getStartParameter().getTaskNames()
                .stream().anyMatch(s->s.contains(XapiPublish.LIFECYCLE_TASK)
                    || "xP".equals(s)
                    || s.endsWith(":xP")
                    || "true".equals(p.findProperty("always.publish"))
                );
            final Action<? super PublishToMavenRepository> config =
                (PublishToMavenRepository ptml) -> {
                    if (ptml.getName().contains("XapiLocal") && !ptml.getName().contains("MainPublication")) {
                        pubXapi.dependsOn(ptml);
                        ptml.dependsOn(LifecycleBasePlugin.ASSEMBLE_TASK_NAME);
                    }
                    ptml.doLast(t->{
                        // TODO: if these tasks are actually in the task graph,
                        // we should pre-clean the directories early,
                        // if the version is a SNAPSHOT...
                        String coords = ptml.getPublication().getGroupId() + ":"
                            + ptml.getPublication().getArtifactId() + ":"
                            + ptml.getPublication().getVersion();
                        p.getGradle().buildFinished(res->{

                            ptml.getLogger().quiet("Published artifact to {} -> {}", ptml.getRepository().getUrl(), coords);
                            ptml.getLogger().trace("Files copied: {}", ptml.getPublication().getArtifacts().stream()
                                .map(PublicationArtifact::getFile)
                                .map(File::getAbsolutePath)
                                .collect(Collectors.joining("/n")));
                        });
                    });
                    // perhaps also make the publish tasks wait on all project build tasks before uploading anything.
                    tasks.getByName("build").finalizedBy(ptml);
                };
            pubXapi.whenSelected(selected->{
                tasks.withType(PublishToMavenRepository.class).all(config);
            });

            if (eager) {
                // eagerly realize all publish tasks, to be sure our lifecycle task actually depends on them.
                tasks.withType(PublishToMavenRepository.class).all(config);
            } else {
                // Only configures publish tasks when created (if you specify hideous publishPubNameToXapiLocalRepository,
                // or otherwise eagerly realize the PublishToMavenRepository tasks)
                tasks.withType(PublishToMavenRepository.class).configureEach(config);
            }
            return publishing.getPublications().create("main", MavenPublication.class,
                pub -> p.getGradle().projectsEvaluated(ready->{
                    String type = (String) p.findProperty("xapi.main.component");
                    final SoftwareComponentContainer comp = p.getComponents();
                    if (type == null) {
                        type = comp.findByName("xapi") == null ? "java" : "xapi";
                    }
                    if ("shadow".equals(type)) {
                        pub.artifact(p.getTasks().getByName("shadowJar"));
                        pub.pom(pom -> {
                            pom.withXml( xml -> {
                                final Node dependenciesNode = xml.asNode().appendNode("dependencies");
                                for (Dependency it : p.getConfigurations().getByName("shadow").getAllDependencies()) {
                                    if (! (it instanceof SelfResolvingDependency)) {
                                        final Node dependencyNode = dependenciesNode.appendNode("dependency");
                                        dependencyNode.appendNode("groupId", it.getGroup());
                                        dependencyNode.appendNode("artifactId", it.getName());
                                        dependencyNode.appendNode("version", it.getVersion());
                                        dependencyNode.appendNode("scope", "runtime");
                                    }
                                }
                            });
                        });
                    } else {
                        pub.from(p.getComponents().getByName(type));
                    }
                    final String baseName = ((Jar)p.getTasks().getByName("jar")).getArchiveBaseName().get();
                    pub.setArtifactId(baseName);

                })
            );

    }
}
