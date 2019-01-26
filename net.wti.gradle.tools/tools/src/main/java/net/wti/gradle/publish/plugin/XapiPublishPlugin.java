package net.wti.gradle.publish.plugin;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.api.XapiLibrary;
import net.wti.gradle.internal.api.XapiPlatform;
import net.wti.gradle.internal.impl.DefaultXapiUsageContext;
import net.wti.gradle.internal.require.api.ArchiveGraph;
import net.wti.gradle.internal.require.api.PlatformGraph;
import net.wti.gradle.internal.require.api.ProjectGraph;
import net.wti.gradle.publish.api.PublishedModule;
import net.wti.gradle.publish.task.XapiPublish;
import net.wti.gradle.system.tools.GradleMessages;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
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

/**
 * Sets up publishing layer,
 * and adds the xapiPublish {} extension.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 1/22/19 @ 3:51 AM.
 */
public class XapiPublishPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        final ProjectView p = ProjectView.fromProject(project);


        // enable IDE to find where we create this object.
        assert GradleMessages.noOpForAssertion(()-> new XapiLibrary(p));
        final XapiLibrary lib = p.getInstantiator().newInstance(XapiLibrary.class, p);

        final TaskProvider<XapiPublish> publishProvider = configureLibrary(p, lib);

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
        project.whenFinalized(p-> {
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
            publish.dependsOn(select.getJavacTask().get());
            publish.dependsOn(select.getJarTask().get());
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
        lib.getPlatforms().all(p->{
            p.getModules().all(m->{
//                if ("main".equals(p.getName()) && "main".equals(m.))
                    publications.create(p.getName()+"_"+m.getName(), MavenPublication.class, pub->{
                        pub.from(m);
                        pub.setGroupId(m.getGroup());
                        pub.setArtifactId(m.getModuleName());
                    });

            });
        });
        // publish the main artifact
        publications.create("xapi", MavenPublication.class)
            .from(lib);

        xapiPublish.configure(pub->
            pub.whenSelected(selected->
                {
                    // publish task was selected, eagerly realize all publishing tasks
                    // TODO: be able to filter these, in case user is other publications.
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
