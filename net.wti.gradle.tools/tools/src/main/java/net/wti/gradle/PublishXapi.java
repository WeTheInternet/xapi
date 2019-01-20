package net.wti.gradle;

import groovy.util.Node;
import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.api.XapiLibrary;
import net.wti.gradle.internal.api.XapiPlatform;
import net.wti.gradle.internal.impl.DefaultXapiUsageContext;
import net.wti.gradle.internal.require.api.ArchiveGraph;
import net.wti.gradle.internal.require.api.PlatformGraph;
import net.wti.gradle.internal.require.api.ProjectGraph;
import net.wti.gradle.system.api.RealizableNamedObjectContainer;
import net.wti.gradle.system.service.GradleService;
import net.wti.gradle.system.tools.GradleMessages;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.SelfResolvingDependency;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository.MetadataSources;
import org.gradle.api.attributes.Usage;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.publish.PublicationArtifact;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class PublishXapi {

    public static final String LIFECYCLE_TASK = "publishXapi";
    public static final String MAIN_PUBLICATION = "xapiMain";

    public static Task getPublishXapiTask(Project project) {
        return GradleService.buildOnce(project, LIFECYCLE_TASK, p->{
            if ("true".equals(p.findProperty("xapi.publish.legacy"))) {
                addPublishXapiTask(project);
            } else {
                final ProjectView view = ProjectView.fromProject(project);
                addPublishXapiLibrary(view);
            }
            return project.getTasks().getByName(LIFECYCLE_TASK);
        });
    }

    public static XapiLibrary addPublishXapiLibrary(ProjectView p) {
        return GradleService.buildOnce(p, XapiLibrary.EXT_NAME, ignored -> {

                assert GradleMessages.noOpForAssertion(()-> new XapiLibrary(p));
                // enable IDE to find where we create this object.
                final XapiLibrary lib = p.getInstantiator().newInstance(XapiLibrary.class, p);

                configureLibrary(p, lib);
                return lib;
            }
        );
    }

    private static void configureLibrary(ProjectView view, XapiLibrary lib) {
        // Create a xapiPublish lifecycle task which, when selected,
        // will fully realize your schemas, detect which artifacts do and don't exist,
        // and generate publications / metadata so the exported configurations are addressable externally.

        // Since build-local dependencies can benefit from XapiRequire realizing on-demand,
        // it is only when we need to publish to an external repo that we want to pay for a full schema realization.

        // We eagerly create the task itself, because we want to install some whenSelected handlers,
        // so if we merely register the task, it could be realized _after_ the task graph is finalized.
        final Task pX = view.getTasks().maybeCreate(LIFECYCLE_TASK);
//        pX.dependsOn(view.getTasks().getByName(BasePlugin.ASSEMBLE_TASK_NAME));
        pX.whenSelected(publishXapi ->{
            publishXapi.dependsOn(view.getTasks().getByName(BasePlugin.ASSEMBLE_TASK_NAME));
            final ProjectGraph project = view.getProjectGraph();
            project.platforms().all(platformGraph -> {
                Map<String, ArchiveGraph> created = new LinkedHashMap<>();
                final RealizableNamedObjectContainer<ArchiveGraph> modules = platformGraph.archives();
                final int[] state = {0};
                final Map<String, ArchiveGraph> selectable = new LinkedHashMap<>();
                modules.configureEach(module -> {
                    // we'll reuse one handler to do three different things.
                    switch (state[0]) {
                        case 0:
                            // Anything in the collection is noted as already-created
                            created.put(module.getPath(), module);
                            // intentional fall-through
                        case 1:
                            if (shouldSelect(module)) {
                                selectable.put(module.getPath(), module);
                            }
                            break;
                        default:
                            throw new IllegalStateException("Module added after publishing finalized: " + module);
                    }
                });
                state[0]++;
                created.clear();
                modules.realize();
                state[0]++;
                finalizeLibrary(view, lib, publishXapi, created, selectable);
            });

        });

    }

    private static void finalizeLibrary(
        ProjectView view,
        XapiLibrary lib,
        Task publishXapi,
        Map<String, ArchiveGraph> created,
        Map<String, ArchiveGraph> selectable
    ) {
        for (ArchiveGraph select : selectable.values()) {
            final PlatformGraph platformGraph = select.platform();
            final XapiPlatform platform = lib.getPlatform(platformGraph.getName());
            final DefaultXapiUsageContext compileCtx = new DefaultXapiUsageContext(select, Usage.JAVA_API);
            final DefaultXapiUsageContext runtimeCtx = new DefaultXapiUsageContext(select, Usage.JAVA_RUNTIME);
            platform.getUsages().add(compileCtx);
            platform.getUsages().add(runtimeCtx);
            publishXapi.dependsOn(select.getSrcName() + "Jar");
        }

        view.getComponents().add(lib);

    }

    private static boolean shouldSelect(ArchiveGraph module) {
        // Anything with source to publish is selectable.
        // Should also likely include anything that has been xapiRequire'd.
        // Should also check for a platform-only build to filter ignored platforms.
        return module.srcExists();
    }

    public static MavenPublication addPublishXapiTask(Project p) {
        return GradleService.buildOnce(p, MAIN_PUBLICATION, PublishXapi::createPublishXapiTask);
    }

    private static MavenPublication createPublishXapiTask(Project p) {
            Object home = p.getExtensions().findByName("xapiHome");
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
            final DefaultTask pubXapi = tasks.create(LIFECYCLE_TASK, DefaultTask.class);
            pubXapi.setGroup("Publishing");
            pubXapi.setDescription("Publish all artifacts destined for the XapiLocal repo");

            // we'll eagerly realize if command line had publishXapi or pX as explicit tasks.
            // not being able to query-then-mutate the task graph kinda sucks,
            // but it should at least make race conditions less murderous.
            boolean eager = p.getGradle().getStartParameter().getTaskNames()
                .stream().anyMatch(s->s.contains(LIFECYCLE_TASK)
                    || "pX".equals(s)
                    || s.endsWith(":pX")
                    || "true".equals(p.findProperty("always.publish"))
                );
            final Action<? super PublishToMavenRepository> config =
                (PublishToMavenRepository ptml) -> {
                    if (ptml.getName().contains("XapiLocal")) {
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

                            ptml.getLogger().info("Published artifact to {} -> {}", ptml.getRepository().getUrl(), coords);
                            ptml.getLogger().trace("Files copied: {}", ptml.getPublication().getArtifacts().stream()
                                .map(PublicationArtifact::getFile)
                                .map(File::getAbsolutePath)
                                .collect(Collectors.joining("/n")));
                        });
                    });
                    // perhaps also make the publish tasks wait on all project build tasks before uploading anything.
                    tasks.getByName("build").finalizedBy(ptml);
                };
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
                    if (type == null) {
                        type = "java";
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
