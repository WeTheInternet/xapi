package net.wti.gradle.publish.plugin;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.api.ReadyState;
import net.wti.gradle.internal.api.XapiLibrary;
import net.wti.gradle.internal.api.XapiPlatform;
import net.wti.gradle.internal.impl.DefaultXapiUsageContext;
import net.wti.gradle.internal.require.api.ArchiveGraph;
import net.wti.gradle.internal.require.api.PlatformGraph;
import net.wti.gradle.internal.require.api.ProjectGraph;
import net.wti.gradle.publish.api.PublishedModule;
import net.wti.gradle.publish.task.XapiPublish;
import net.wti.gradle.system.plugin.XapiBasePlugin;
import net.wti.gradle.system.tools.GradleMessages;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository.MetadataSources;
import org.gradle.api.attributes.Usage;
import org.gradle.api.initialization.IncludedBuild;
import org.gradle.api.internal.GradleInternal;
import org.gradle.api.publish.PublicationContainer;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.ivy.tasks.PublishToIvyRepository;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository;
import org.gradle.api.publish.plugins.PublishingPlugin;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.TaskReference;

import java.io.File;

/**
 * Sets up publishing layer,
 * and adds the xapiPublish {} extension.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 1/22/19 @ 3:51 AM.
 */
@SuppressWarnings("UnstableApiUsage")
public class XapiPublishPlugin implements Plugin<Project> {

    public static final String XAPI_LOCAL = "xapiLocal";

    @Override
    public void apply(Project project) {
        project.getPlugins().apply(XapiBasePlugin.class);
        final ProjectView p = ProjectView.fromProject(project);

        // enable IDE to find where we create this object.
        assert GradleMessages.noOpForAssertion(()-> new XapiLibrary(p));
        final XapiLibrary lib = p.getInstantiator().newInstance(XapiLibrary.class, p);

        final TaskProvider<XapiPublish> publishProvider = configureLibrary(p, lib);
        configureRepo(p, lib);

        if (((GradleInternal)p.getGradle()).getRoot() == p.getGradle()) {
            publishProvider.configure(publish->{
                for (IncludedBuild inc : p.getGradle().getIncludedBuilds()) {
                    final TaskReference childPublish = inc.task(":publishRequired");
                    publish.dependsOn(childPublish);
                }
            });
        } else {
            final TaskContainer rootTasks = p.getRootProject().getTasks();
            final Task required = rootTasks.maybeCreate("publishRequired");
            required.whenSelected(publishRequired->publishRequired.dependsOn(publishProvider));
        }
    }

    private void configureRepo(ProjectView view, XapiLibrary lib) {

        view.getProjectGraph().whenReady(ReadyState.AFTER_CREATED, created -> {
            String repo = (String) view.findProperty("xapi.mvn.repo");
            final RepositoryHandler repos = view.getPublishing().getRepositories();
            if (repo == null) {
                final boolean addRepo = repos.stream().noneMatch(r->XAPI_LOCAL.equals(r.getName()));
                if (addRepo) {
                    String xapiHome = (String) view.findProperty("xapi.home");
                    if (xapiHome == null) {
                        xapiHome = view.getService().getXapiHome();
                    }
                    repo = new File(xapiHome , "repo").toURI().toString();
                } else {
                    return;
                }
            }
            String xapiRepo = repo;
            repos.maven(mvn -> {
                mvn.setUrl(xapiRepo);
                mvn.setName("xapiLocal");
                mvn.metadataSources(MetadataSources::gradleMetadata);
            });
        });
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
        // xapiPublish.dependsOn(view.getTasks().getByName(BasePlugin.ASSEMBLE_TASK_NAME));

//        // workaround...
//        view.getRootProject().getTasks().maybeCreate("publishRequired");

        final ProjectGraph project = view.getProjectGraph();

        final boolean[] canMutate = {true}; // TODO use MutationGuard instead
        project.platforms().configureEach(platformGraph -> {
            platformGraph.archives().configureEach(module -> {
                if (canMutate[0]) {
                    if (shouldSelect(module)) {
                        selectModule(view, lib, xapiPublish, module);
                    }
                } else {
                    throw new IllegalStateException("Module added after publishing finalized: " + module);
                }
            });

        });
        project.whenReady(ReadyState.AFTER_READY, p-> {
            finalizeLibrary(view, lib, xapiPublish);
            canMutate[0] = false;
        });
        return xapiPublish;

    }

    private void selectModule(ProjectView view, XapiLibrary lib, TaskProvider<XapiPublish> xapiPublish, ArchiveGraph select) {
        final PlatformGraph platformGraph = select.platform();
        final XapiPlatform platform = lib.getPlatform(platformGraph.getName());
        PublishedModule module = platform.getModule(select.getName());
        // This stuff is happening too late.
        // Needs to occur right away / be build on demand...
        final DefaultXapiUsageContext compileCtx = new DefaultXapiUsageContext(select, Usage.JAVA_API);
        final DefaultXapiUsageContext runtimeCtx = new DefaultXapiUsageContext(select, Usage.JAVA_RUNTIME);
        module.getUsages().add(compileCtx);
        module.getUsages().add(runtimeCtx);
        // The produced artifacts will be added to publication based on their presence,
        // but we need to realize the tasks for them to run callbacks and hook themselves up.
        // So, we make the lifecycle task depend on the assembled configuration.
        xapiPublish.configure(publish -> {
            publish.whenSelected(selected-> {
                publish.dependsOn(select.getJavacTask().get());
                publish.dependsOn(select.getJarTask().get());
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
        lib.getPlatforms().all(p->
            p.getModules().all(m->
                    publications.create(p.getName()+"_"+m.getName(), MavenPublication.class, pub->{
                        pub.from(m);
                        pub.setGroupId(m.getGroup());
                        pub.setArtifactId(m.getModuleName());
                        // "generatePomFileFor" + capitalize(publicationName) + "Publication"

                    })

            )
        );
        // publish the main artifact.  All items above are "children" / variants of the XapiLibrary, lib
        publications.create("xapi", MavenPublication.class).from(lib);

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
        return module.srcExists();
    }

}
