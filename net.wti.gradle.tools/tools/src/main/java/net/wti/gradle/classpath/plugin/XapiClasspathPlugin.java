package net.wti.gradle.classpath.plugin;

import net.wti.gradle.classpath.tasks.XapiClasspathTask;
import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.api.ReadyState;
import net.wti.gradle.internal.require.api.*;
import net.wti.gradle.system.tools.GradleCoerce;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;

import static net.wti.gradle.system.tools.GradleCoerce.toTitleCase;

/**
 * XapiClasspathPlugin (xapi-classpath):
 * <p>
 * <p>
 * <p> Activating this plugin will register a {@link XapiClasspathTask} per realized platform+module.
 * <p>
 * <p> LATER: have xapi-require use feature detection of xapi-classpath plugin to decide to use our generated XapiClasspathTasks
 * <p> For the actual compile / runtime classpaths of javaCompile / etc tasks.
 * <p> That is, if xapi-classpath is present, ask it to create a task using standard naming,
 * <p> otherwise, xapi-require can either create it's own derived classpath task, or just resolve classpath configuration directly.
 * <p>
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 09/03/2021 @ 3:22 a.m..
 */
@SuppressWarnings("UnstableApiUsage")
public class XapiClasspathPlugin implements Plugin<Project> {


    @Override
    public void apply(final Project project) {
        final ProjectView view = ProjectView.fromProject(project);
        final ProjectGraph pg = view.getProjectGraph();
        pg.whenReady(ReadyState.AFTER_CREATED - 0x80, ready -> {
            pg.realizedPlatforms(plat -> {
                plat.realizedArchives(mod -> {
                    project.getLogger().trace("Realized module {} @ {}-{}\n" +
                            "Incoming: {}\n" +
                            "Outgoing: {}\n", project, plat.getName(), mod.getName(), mod.getIncoming(), mod.getOutgoing());
                });
            });
            for (PlatformGraph platform : pg.platforms()) {
                for (ArchiveGraph module : platform.archives()) {
                    if (module.realized()) {
                        module.configExportedApi();
                        module.configExportedRuntime();
                        if (module.config().isSourceAllowed() || platform.config().isRequireSource()) {
                            module.configExportedSource();
                        }
                        String classpathTaskName = "classpath" +
                                ("main".equals(platform.getName()) ? "" : toTitleCase(platform.getName())) +
                                toTitleCase(module.getName());
                        // TODO: some configurable guidance on naming and classpath sources?
//                        project.getTasks().create(classpathTaskName, XapiClasspathTask.class, cp -> {
                        project.getTasks().register(classpathTaskName, XapiClasspathTask.class, cp -> {
                            cp.getPlatform().set(platform.getName());
                            cp.getModule().set(module.getName());
                            cp.getUsageType().set(DefaultUsageType.Runtime);
                        });
//                        project.getTasks().register(classpathTaskName + "Runtime", XapiClasspathTask.class, cp -> {
//                                cp.getClasspath().from( module.configExportedRuntime() );
//                        });
                    } else {
                        view.getLogger().quiet("SKIPPING UNREALIZED " + module.getPath() + " in " + pg.getPath());
                    }
                }

            }

        });


    }
}
