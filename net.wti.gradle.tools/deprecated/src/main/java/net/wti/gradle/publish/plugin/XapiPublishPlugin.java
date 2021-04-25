package net.wti.gradle.publish.plugin;

import net.wti.gradle.internal.api.*;
import net.wti.gradle.internal.impl.DefaultXapiUsageContext;
import net.wti.gradle.internal.require.api.ArchiveGraph;
import net.wti.gradle.internal.require.api.PlatformGraph;
import net.wti.gradle.internal.require.api.ProjectGraph;
import net.wti.gradle.publish.api.PublishedModule;
import net.wti.gradle.publish.impl.XapiLibrary;
import net.wti.gradle.publish.task.XapiPublish;
import net.wti.gradle.system.plugin.XapiBasePlugin;
import net.wti.gradle.system.tools.GradleMessages;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.attributes.Usage;
import org.gradle.api.initialization.IncludedBuild;
import org.gradle.api.internal.GradleInternal;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.publish.PublicationContainer;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.ivy.tasks.PublishToIvyRepository;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository;
import org.gradle.api.publish.plugins.PublishingPlugin;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.TaskReference;
import xapi.gradle.fu.LazyString;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Sets up publishing layer,
 * and adds the xapiPublish {} extension.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 1/22/19 @ 3:51 AM.
 */
@SuppressWarnings({"UnstableApiUsage", "unused"})
public class XapiPublishPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        final XapiBasePlugin base = project.getPlugins().apply(XapiBasePlugin.class);

        final ProjectView view = ProjectView.fromProject(project);

        // enable IDE to find where we create this object.
        assert GradleMessages.noOpForAssertion(()-> new XapiLibrary(view));
        final XapiLibrary lib = view.getInstantiator().newInstance(XapiLibrary.class, view);

        final TaskProvider<XapiPublish> publishProvider = configureLibrary(view, lib);

        if (((GradleInternal) view.getGradle()).getRoot() == view.getGradle()) {
            publishProvider.configure(publish->{
                for (IncludedBuild inc : view.getGradle().getIncludedBuilds()) {
                    final TaskReference childPublish = inc.task(":publishRequired");
                    publish.dependsOn(childPublish);
                }
            });
        } else {
            final TaskContainer rootTasks = view.getRootProject().getTasks();
            final Task required = rootTasks.maybeCreate("publishRequired");
            required.whenSelected(publishRequired->publishRequired.dependsOn(publishProvider));
        }

        // Add xapiLocal to the *publishing* repositories.
        base.configureRepo(view.getPublishing().getRepositories(), project);
    }

    private TaskProvider<XapiPublish> configureLibrary(ProjectView view, XapiLibrary lib) {
        // Create a xapiPublish lifecycle task which, when selected,
        // will fully realize your schemas, detect which artifacts do and don't exist,
        // and generate publications / metadata so the exported configurations are addressable externally.

        // Since build-local dependencies can benefit from XapiRequire realizing on-demand,
        // it is only when we need to publish to an external repo that we want to pay for a full schema realization.

        // We eagerly create the task itself, because we want to install some whenSelected handlers,
        // so if we merely register the task, it could be realized _after_ the task graph is finalized.
        final TaskProvider<XapiPublish> xapiPublish = view.getTasks().register(XapiPublish.LIFECYCLE_TASK, XapiPublish.class);
        // depending on assemble is an escape hatch we don't really need (atm)
        // plus it's better to let people choose assemble.dependsOn xapiPublish, if they wish.
        // xapiPublish.dependsOn(view.getTasks().getByName(BasePlugin.ASSEMBLE_TASK_NAME));

        final ProjectGraph project = view.getProjectGraph();

        project.platforms().configureEach(platformGraph -> {
            final boolean[] canMutate = {true}; // TODO use MutationGuard instead
            platformGraph.archives().configureEach(module -> {
                if (!canMutate[0]) {
                    throw new IllegalStateException("Module added to " + view.getPath() + " after publishing finalized: " + module);
                }
                project.whenReady(ReadyState.AFTER_FINISHED, p-> {
                    canMutate[0] = false;
                    if (shouldSelect(module)) {
                        selectModule(view, lib, xapiPublish, module);
                    }
                });
            });

        });
        project.whenReady(ReadyState.AFTER_FINISHED + 0x10, p-> {
            finalizeLibrary(view, lib, xapiPublish);
        });
        return xapiPublish;

    }

    private void selectModule(ProjectView view, XapiLibrary lib, TaskProvider<XapiPublish> xapiPublish, ArchiveGraph select) {
        final PlatformGraph platformGraph = select.platform();
        final XapiPlatform platform = lib.getPlatform(platformGraph.getName());
        PublishedModule module = platform.getModule(select.getName());

        final DefaultXapiUsageContext compileCtx = new DefaultXapiUsageContext(select, Usage.JAVA_API);
        final DefaultXapiUsageContext runtimeCtx = new DefaultXapiUsageContext(select, Usage.JAVA_RUNTIME);
        module.getUsages().add(compileCtx);
        module.getUsages().add(runtimeCtx);
        final boolean hasSource = select.config().isSourceAllowed();
        if (hasSource) {
            view.getLogger().info("Adding source jar publishing for {}", select.getCapability());
            final DefaultXapiUsageContext sourceCtx = new DefaultXapiUsageContext(select, XapiUsage.SOURCE_JAR);
            module.getUsages().add(sourceCtx);
        }
        final String pomTaskName = "generatePomFileFor_" + platformGraph.getName() + "_" + select.getName() + "Publication";
            select.getJarTask().configure(jar -> {
                jar.whenSelected(selected->{
                    final TaskProvider<Task> pomTask = view.getTasks().named(pomTaskName);
                    String[] cap = select.getCapability().split(":");
                    jar.into("META-INF/maven/" + cap[0] + "/" + cap[1] + "/pom.xml", spec -> {
                        spec.from(pomTask);
                });
            });
        });
        // The produced artifacts will be added to publication based on their presence,
        // but we need to realize the tasks for them to run callbacks and hook themselves up.
        // So, we make the lifecycle task depend on the assembled configuration.
        xapiPublish.configure(publish -> {
            publish.whenSelected(selected-> {
                publish.dependsOn(select.getJavacTask().get());
                publish.dependsOn(select.getJarTask().get());
                if (hasSource) {
                    publish.dependsOn(select.getTasks().getSourceJarTask());
                }
            });
        });
    }

    private void finalizeLibrary(
        ProjectView view,
        XapiLibrary lib,
        TaskProvider<XapiPublish> xapiPublish
    ) {
        // would be nice to make this conditional,
        // but by the time xapiPublish whenSelected is called,
        // the publishing metadata will already have been realized.
        view.getProjectGraph().platforms().realize();

        if (!lib.isEmpty()) {
            // An empty component will blow up.
            // i.e. if the plugin is applied to a project with no sources / archives built.
            view.getComponents().add(lib);
        }

        PublishingExtension publishing = view.getExtensions().findByType(PublishingExtension.class);
        if (publishing == null) {
            // User has not added publishing plugin.  Help them out.
            // TODO: consider instead applying a plugin for them...
            view.getLogger().info(
                "No publishing plugin applied before xapi-publish; consider applying maven-publish or ivy-publish plugins sooner");
            view.getPlugins().apply(PublishingPlugin.class);
            publishing = view.getExtensions().getByType(PublishingExtension.class);
        }

        final PublicationContainer publications = publishing.getPublications();
        // TODO: add some levers/knobs to dial-back this eager initialization of complete build graph:
        //  user is likely to want to limit publishing to certain platforms at once, at the very least.
        //  Also; try getting this moved inside the whenSelected block below; it was pulled out due to timing issues.
        List<MavenPublication> pubs = new ArrayList<>();
        LinkedHashMap<String, PublishedModule> allMods = new LinkedHashMap<>();
        lib.getPlatforms().all(p->
            p.getModules().all(m-> {
                String key ="_" + p.getName()+"_"+m.getName();
                pubs.add(publications.create(key, MavenPublication.class, pub->{
                    pub.from(m);
                    pub.setGroupId(m.getGroup());
                    pub.setArtifactId(m.getModuleName());
                    allMods.put(m.getGroup() + ":" + m.getModuleName(), m);
                    // "generatePomFileFor" + capitalize(publicationName) + "Publication"
                    ((ExtensionAware)pub).getExtensions().add("xapiModule", m);


                    final String pomTaskName = "generatePomFileFor" + key + "Publication";
                    final TaskProvider<Task> pomTask = view.getTasks().named(pomTaskName);
                    final ArchiveGraph arch = view.getProjectGraph().platform(p.getName()).archive(m.getName());
                    pub.versionMapping(strat -> {
                        strat.usage(XapiUsage.SOURCE_JAR, variant -> {
                            variant.fromResolutionOf(arch.configExportedSource());
                        });
                    });
                    arch.getJarTask().configure(jar -> {
                        String[] cap = arch.getCapability().split(":");
                        jar.into("META-INF/maven/" + cap[0] + "/" + cap[1], spec -> {
                            spec.from(pomTask);
                            spec.rename("pom-default.xml", "pom.xml");
                        });
                    });


                }));
            })
        );
//        // publish the main artifact.  All items above are child modules (~variants) of the XapiLibrary
        // Disabled, as it currently interferes w/ "just get it directly from other publications".
//        MavenPublication mainPub = publications.create("xapi", MavenPublication.class);
//        mainPub.from(lib);

        view.getTasks().configureEach(task-> {
            if (task instanceof PublishToMavenRepository) {
                PublishToMavenRepository pub = (PublishToMavenRepository) task;
                final MavenPublication source = pub.getPublication();

                final Object module = ((ExtensionAware) source).getExtensions().findByName("xapiModule");
                if (module != null) {
                    PublishedModule mod = (PublishedModule) module;
                    final ArchiveGraph graph = mod.getModule();
                    task.whenSelected(selected->{
                        task.dependsOn(graph.getJarTask());
                        if (graph.config().isSourceAllowed()) {
                            task.dependsOn(graph.getSourceJarTask());
                        }
                    });
                }
            }
        });

        // avoid creating the task.
        xapiPublish.configure(pub->
            // Only run this code when the task has actually been selected.
            pub.whenSelected(selected->
                {
                    // publish task was selected, eagerly realize all publishing tasks
                    // TODO: be able to filter these, in case user has other publications.
                    view.getTasks().withType(PublishToMavenRepository.class)
                        .all(pub::dependsOn);

                    view.getTasks().withType(PublishToIvyRepository.class)
                        .all(pub::dependsOn);
                }
                )
        );
    }

    @SuppressWarnings("WeakerAccess")
    protected boolean shouldSelect(ArchiveGraph module) {
        // Anything with source to publish is selectable.
        // Should also likely include anything that has been xapiRequire'd.
        // Should also check for a platform-only build to filter ignored platforms.
        if (module.realized() && module.isSelectable()) {
            final boolean published = module.config().isPublished();
            module.getView().getLogger().trace("Publishing {}? {}", LazyString.nonNullString(module::getModuleName), published);
            return published;
        }
        return false;
    }

}
