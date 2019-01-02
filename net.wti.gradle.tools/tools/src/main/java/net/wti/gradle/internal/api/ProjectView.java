package net.wti.gradle.internal.api;

import net.wti.gradle.internal.impl.DefaultProjectView;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.ArtifactHandler;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.logging.Logger;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.internal.reflect.Instantiator;

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
public interface ProjectView {

    String getPath();
    ObjectFactory getObjects();
    Instantiator getInstantiator();
    ProviderFactory getProviders();
    Logger getLogger();
    TaskContainer getTasks();
    ConfigurationContainer getConfigurations();
    RepositoryHandler getRepositories();
    DependencyHandler getDependencies();
    ArtifactHandler getArtifacts();
    SourceSetContainer getSourceSets();
    Object findProperty(String named);

    static ProjectView fromProject(Project project) {
        final Instantiator inst = ((ProjectInternal) project).getServices().get(Instantiator.class);
        return new DefaultProjectView(inst, project);
    }

}
