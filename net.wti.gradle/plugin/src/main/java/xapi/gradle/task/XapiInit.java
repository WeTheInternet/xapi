package xapi.gradle.task;

import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.gradle.util.GFileUtils;
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
    private volatile boolean use;
    private final RegularFileProperty initFile;
    private final Property<String> initFileName;

    public XapiInit() {
        postInit = getProject().getTasks().register(POST_INIT_TASK_NAME, task -> {
            use = true;
            task.dependsOn(this);
            if (getState().isConfigurable()) {
                finalizedBy(task);
            }
        });
        preInit = getProject().getTasks().register(PRE_INIT_TASK_NAME, task-> {
            use = true;
            if (getState().isConfigurable()) {
                dependsOn(task);
            }
        });
        onlyIf(t-> use || !t.getActions().isEmpty());
        initFile = getProject().getObjects().fileProperty();
        initFileName = getProject().getObjects().property(String.class);
        initFileName.set(".xapi.initFile");
        initFile.set(getProject().getLayout().getBuildDirectory().file(initFileName));
    }

    @Input
    public Property<String> getInitFileName() {
        return initFileName;
    }

    @OutputFile
    public RegularFileProperty getInitFile() {
        return initFile;
    }

    public void configure(XapiExtension ext) {

    }

    @TaskAction
    public void doInit() {
        GFileUtils.writeFile(getInitFileContents(), initFile.getAsFile().get(), "UTF-8");
    }

    protected String getInitFileContents() {
        return Long.toString(System.currentTimeMillis());
    }

    public TaskProvider<Task> getPostInit() {
        // forcibly realize the tasks, even if they weren't originally requested
        // (this wiring also happens if you directly invoke pre/post init task.
        finalizedBy(postInit);
        return postInit;
    }

    public TaskProvider<Task> getPreInit() {
        dependsOn(preInit);
        return preInit;
    }
}
