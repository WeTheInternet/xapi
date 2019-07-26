package xapi.gradle.plugin;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.system.service.GradleService;
import net.wti.gradle.system.tools.GradleMessages;
import org.gradle.BuildAdapter;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.*;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.internal.concurrent.ExecutorFactory;
import org.gradle.internal.concurrent.ManagedExecutor;
import xapi.fu.Lazy;
import xapi.gradle.task.XapiInit;
import xapi.gradle.task.XapiPreload;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 11/4/18 @ 3:04 AM.
 */
public class XapiRootPlugin implements Plugin<Project> {

    private final ExecutorFactory exec;

    @Inject
    public XapiRootPlugin(ExecutorFactory exec) {
        this.exec = exec;
    }


    @Override
    public void apply(Project project) {
        if (project != project.getRootProject()) {
            throw new GradleException("xapi-root plugin may only be installed on the root project!");
        }
        ProjectView view = ProjectView.fromProject(project);

        view.getService(); // ensure the GradleService is initialized

        final XapiExtensionRoot root = project.getExtensions().create(
            XapiExtensionRoot.EXT_NAME, XapiExtensionRoot.class, project);
        //noinspection ConstantConditions just here to trace "real" constructor call, above, in IDE
        assert GradleMessages.noOpForAssertion(()->new XapiExtensionRoot(project)) : "";

        // okay...  scan for the following files:
        // settings.xapi, and, per-gradle-project, schema.xapi
        loadSettings();
        loadSchemas();


        project.getGradle().addBuildListener(new BuildAdapter(){
            @Override
            public void projectsEvaluated(Gradle gradle) {
                final List<ModuleDependency> local = root.getLocalCache().getOrNull();
                // For all dependencies requested, pre-populate the $rootDir/repo
                if (local != null && !local.isEmpty()) {
                    preload(project, root, local);
                }
            }
        });
    }

    private void loadChildSchemas() {

    }

    private void loadSchemas() {

    }

    private void loadSettings() {
        // settings.xapi will contain build-wide metadata.
        // This includes the version / source repository metadata for preloadable dependencies.
        // This will also, likely, include the full subproject structure;
        // for use by xapi-settings plugin, to be able to define gradle projects per settings.xapi layout.
    }

    /**
     * TODO move this method into {@link GradleService} so it can control how this extension is created.
     */
    private XapiExtensionRoot createRootExtension(Project project) {
        return project.getExtensions().create(
            XapiExtensionRoot.EXT_NAME,
            XapiExtensionRoot.class,
            project
        );
    }

    protected void preload(
        Project project,
        XapiExtensionRoot root,
        List<ModuleDependency> local
    ) {
        final Configuration detached = project.getConfigurations()
            .detachedConfiguration(local.toArray(new Dependency[0]));
        // TODO: also allow configuration of custom sources / repositories for preload-only.
        Lazy<ResolvedConfiguration> resolve = Lazy.deferred1(detached::getResolvedConfiguration);
        final ManagedExecutor exe = exec.create("xapi-preload", 1);
        exe.submit(resolve.asCallable());

        final TaskProvider<XapiPreload> pre = project.getTasks().register(
            "xapiPreload",
            XapiPreload.class,
            preload -> {
                Callable<Object> defer = () -> {
                    final ResolvedConfiguration resolved = resolve.out1();
                    if (resolved.hasError() && root.isStrict()) {
                        resolved.rethrowFailure();
                    }
                    final LenientConfiguration lenient = resolved.getLenientConfiguration();

                    preload.addArtifacts(lenient);
                    return lenient.getFiles();
                };

                // using the callable as source mainly just to get input file tracking,
                // and work-deferment, of course...
                preload.source(defer);
            }
        );
        // Make the preInit task depend on preload (so it happens as early as possible).
        project.getTasks().configureEach(t->{
            // TODO: make a generalized "forTaskNamed()" function so we can stay lazily evaluated
            // without adding 700 configureEach callbacks.
            if (XapiInit.INIT_TASK_NAME.equals(t.getName())) {
                ((XapiInit)t).getPreInit().get().dependsOn(pre);
            }
        });
    }
}
