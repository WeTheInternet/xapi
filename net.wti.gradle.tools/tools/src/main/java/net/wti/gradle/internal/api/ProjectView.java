package net.wti.gradle.internal.api;

import net.wti.gradle.api.MinimalProjectView;
import net.wti.gradle.internal.ProjectViewInternal;
import net.wti.gradle.internal.impl.DefaultProjectView;
import net.wti.gradle.internal.require.api.BuildGraph;
import net.wti.gradle.internal.require.api.ProjectGraph;
import net.wti.gradle.schema.api.XapiSchema;
import net.wti.gradle.system.service.GradleService;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.ArtifactHandler;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.component.SoftwareComponentContainer;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.initialization.Settings;
import org.gradle.api.internal.CollectionCallbackActionDecorator;
import org.gradle.api.internal.GradleInternal;
import org.gradle.api.internal.artifacts.dsl.DefaultComponentMetadataHandler;
import org.gradle.api.internal.artifacts.dsl.dependencies.ProjectFinder;
import org.gradle.api.internal.attributes.ImmutableAttributesFactory;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.logging.Logger;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.internal.build.BuildState;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.util.GUtil;

import java.io.File;
import java.util.concurrent.Callable;

/**
 * A "lightweight view" of commonly-passed-around objects from a given gradle {@link Project}.
 *
 * We'll instead pass around a single view object for all of these,
 * to avoid cluttering method signatures with mutable Project object.
 *
 * This also makes it feasible for you customize this context object, if you so desire.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 12/31/18 @ 12:45 AM.
 */
public interface ProjectView extends ProjectViewInternal {

    String EXT_NAME = System.getProperty("xapi.view", "_xapi");

    String getPath();
    ObjectFactory getObjects();
    ProviderFactory getProviders();
    TaskContainer getTasks();
    ConfigurationContainer getConfigurations();
    RepositoryHandler getRepositories();
    DependencyHandler getDependencies();
    SoftwareComponentContainer getComponents();
    ArtifactHandler getArtifacts();
    SourceSetContainer getSourceSets();
    Object findProperty(String named);
    ProjectView findProject(String named);
    ImmutableAttributesFactory getAttributesFactory();
    PublishingExtension getPublishing();
    String getName();
    PluginContainer getPlugins();
    BuildGraph getBuildGraph();
    FileTree zipTree(Object from);
    GradleService getService();
    DefaultComponentMetadataHandler getComponentMetadata();
    ProjectLayout getLayout();

    static ProjectView fromProject(Project project) {
        return GradleService.buildOnce(ProjectView.class, project, ProjectView.EXT_NAME, ignored-> {
            ProjectInternal p = (ProjectInternal) project;
            final Instantiator inst = p.getServices().get(Instantiator.class);
            final CollectionCallbackActionDecorator dec = p.getServices().get(CollectionCallbackActionDecorator.class);
            final ProjectFinder finder = p.getServices().get(ProjectFinder.class);
            final ImmutableAttributesFactory attributesFactory = p.getServices().get(ImmutableAttributesFactory.class);
            return new DefaultProjectView(project, inst, dec, finder, attributesFactory);
        });
    }

    default XapiSchema getSchema() {
        return GradleService.buildOnce(this, XapiSchema.EXT_NAME, XapiSchema::new);
    }

    default NamedDomainObjectProvider<ProjectGraph> projectGraph() {
        return getBuildGraph().project(getPath());
    }

    default ProjectGraph getProjectGraph() {
        return projectGraph().get();
    }

    @Override
    default ProjectView getRootProject() {
        return findProject(":");
    }


    default File getProjectDir() {
        return getLayout().getProjectDirectory().getAsFile();
    }

    default File getBuildDir() {
        return getLayout().getProjectDirectory().getAsFile();
    }

    default File file(String path) {
        return new File(getProjectDir(), path);
    }

    ConfigurableFileCollection files(Object ... from);

    default Dependency dependencyFor(String path, Configuration config) {
        final DependencyHandler deps = getDependencies();
        return deps.project(GUtil.map(
                "path", path,
                // Important: We are not adding the configuration itself;
                // we are adding a dependency to the same-named configuration
                // in a possibly different project.
                "configuration", config.getName()
            )
        );
    }

    default <T> Provider<T> lazyProvider(Callable<T> items) {
        return lazyProvider(getProviders().provider(items));
    }

    default <T> Provider<T> lazyProvider(Provider<T> items) {
        return lazyProvider(items, false);
    }

    /**
     * Wraps the provider in run-once semantics.
     *
     * @param items Your provider.
     * @param strict if true, we will add an assert, ensuring your provider does not change after it has been read.
     * @param <T> The type of provider you are supplying / want returned.
     * @return A provider which will only run once.
     */
    @SuppressWarnings({"unchecked", "AssertWithSideEffects"})
    default <T> Provider<T> lazyProvider(Provider<T> items, boolean strict) {
        Object[] result = {null};
        return getProviders().provider(()->{
            final Object is;
            synchronized (result) {
                is = result[0];
                if (is == null) {
                    result[0] = items.get();
                } else if (strict){
                    Object tmp;
                    assert is == (tmp=items.get()) : "Cannot reassign " + is + " to " + tmp;
                }
            }

            return (T)result[0];
        });
    }

    default boolean isJavaCompatibility() {
        return getPlugins().hasPlugin(JavaPlugin.class);
    }


    default String getDebugPath() {
        return getPath() +
            " of " + GradleService.buildId(getGradle()) +
            " at " + getProjectDir().toURI();
    }

    default SourceSet getMainSources() {
        return getProjectGraph().mainModule().getSource().getSrc();
    }

    @Override
    default ProjectViewInternal findView(String name) {
        if (!name.startsWith(":")) {
            name = ":" + name;
        }
        if (name.equals(getPath())) {
            return this;
        }
        return findProject(name);
    }

    @Override
    default void whenSettingsReady(Action<Settings> callback) {
        callback.execute(((GradleInternal)getGradle()).getSettings());
    }

    void ensureEvaluated();

    @Override
    default String getBuildName() {
        return ProjectViewInternal.super.getBuildName();
    }

}
