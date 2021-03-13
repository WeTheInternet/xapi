package net.wti.gradle.classpath.plugin;

import net.wti.gradle.classpath.tasks.XapiClasspathTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

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
public class XapiClasspathPlugin implements Plugin<Project> {


    @Override
    public void apply(final Project project) {



    }
}
