package xapi.gradle.plugin;

import net.wti.gradle.system.service.GradleService;
import net.wti.gradle.system.spi.GradleServiceFinder;
import org.gradle.BuildAdapter;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.*;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.internal.reflect.Instantiator;
import xapi.fu.Lazy;
import xapi.fu.Out1;
import xapi.gradle.task.XapiInit;
import xapi.gradle.task.XapiPreload;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 11/4/18 @ 3:04 AM.
 */
public class XapiRootPlugin implements Plugin<Project> {

    private final Instantiator instantiator;

    @Inject
    public XapiRootPlugin(Instantiator instantiator) {
        this.instantiator = instantiator;
    }



    @Override
    public void apply(Project project) {
        if (project != project.getRootProject()) {
            throw new GradleException("xapi-root plugin may only be installed on the root project!");
        }
        final GradleService service = GradleServiceFinder.getService(project);

        final XapiExtensionRoot root = createRootExtension(project, instantiator);
        project.getExtensions().add(XapiExtension.class, XapiExtension.EXT_NAME, root);

        project.getPlugins().apply(XapiBasePlugin.class);

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

        // We're actually just running a GradleBuild in the buildSrc project to ensure plugin jars are _always_ fresh.
//        project.getGradle().getIncludedBuilds().forEach(
//            b -> {
//            if ("net.wti.gradle".equals(b.getName())) {
//                XapiExtension ext = (XapiExtension) project.getExtensions().getByName(XapiExtension.EXT_NAME);
//                project.getLogger().quiet("Accessing init task so we can encourage up-to-date maven dependencies");
//                ext.getInit().dependsOn(b.task(":xapi-gradle-plugin:publishToMavenLocal"));
//            }
//        });

    }

    /**
     * TODO move this method into {@link GradleService} so it can control how this extension is created.
     */
    private XapiExtensionRoot createRootExtension(Project project, Instantiator instantiator) {
        return project.getExtensions().create(
            XapiExtensionRoot.EXT_NAME,
            XapiExtensionRoot.class,
            project,
            instantiator
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
        // Using an inline class here mostly for nice names when debugging threads.
        class PreloadThread extends Thread {
            private PreloadThread() {
                setName("xapi-preload-thread");
                setDaemon(true);
                setPriority(Thread.MIN_PRIORITY+2);
                setUncaughtExceptionHandler((t, e)->{
                    project.getLogger().error("Xapi preload failed; expect more errors", e);
                });
            }
            @Override
            public void run() {
                resolve.out1();
            }
        }
        // kick off the preloads eagerly.
        new PreloadThread().start();

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
