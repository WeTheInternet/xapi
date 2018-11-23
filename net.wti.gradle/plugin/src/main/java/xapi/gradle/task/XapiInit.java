package xapi.gradle.task;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;
import xapi.fu.Lazy;
import xapi.gradle.plugin.XapiExtension;

/**
 * A "lifecycle task with benefits".
 *
 * xapiInit is installed into each project by {@link xapi.gradle.plugin.XapiBasePlugin},
 * and accessible to any xapi extension via:
 * xapi.initProvider.configure { } // use task provider; preferred
 * xapi { init { doLast {...} }} // use dsl that uses task provider; decent choice
 * xapi { init.finalizedBy sweetTaskOfMine } // Deprecated. Actually realize the init task.  Avoid unless wholly necessary.
 *
 * You may also use:
 * xapi.beforeInit { initTask -> return nullOrATaskProvider }
 * xapi.afterInit { initTask -> return nullOrATaskProvider }
 *
 * These callbacks are special; if you return anything, it must be a TaskProvider or null.
 * When you return non-null:
 * xapiInit.dependsOn beforeInit
 * afterInit.dependsOn xapiInit
 * xapiInit.finalizedBy afterInit
 *
 * You would return null to take advantage of the ordering semantics of the callbacks.
 * beforeInit callbacks run earlier than afterInit callbacks.
 *
 * You may freely add to either set of callbacks any time;
 * if #configure has already been called, your callback will be invoked immediately,
 * and your TaskProvider wired into the task graph.
 *
 * We may also have this task produce a cacheable output artifact, containing build information;
 * we would use the project build files (and .properties files) as inputs,
 * so we could invalidate the xapiInit tasks as appropriate (and anything depending on them).
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 11/11/18 @ 11:07 PM.
 */
public class XapiInit extends DefaultTask {

    public static final String INIT_TASK_NAME = "xapiInit";
    public static final String POST_INIT_TASK_NAME = "xapiPostInit";
    public static final String PRE_INIT_TASK_NAME = "xapiPreInit";

    private final TaskProvider<Task> postInit;
    private final TaskProvider<Task> preInit;

    public XapiInit() {
        postInit = getProject().getTasks().register(POST_INIT_TASK_NAME, task -> {
            task.dependsOn(this);
            finalizedBy(task);
        });
        preInit = getProject().getTasks().register(PRE_INIT_TASK_NAME, this::dependsOn);
    }

    public void configure(XapiExtension ext) {

    }

    @TaskAction
    public void initialize() {
        // We don't actually do anything here.
        // This task exists mostly to give you something to bind task dependencies to.

    }

    public TaskProvider<Task> getPostInit() {
        // forcibly realize the tasks, even if they weren't originally requested
        // (this wiring also happens if you directly invoke pre/post init task.
        finalizedBy(postInit);
        return postInit;
    }

    public TaskProvider<Task> getPreInit() {
        dependsOn(postInit);
        return postInit;
    }
}
