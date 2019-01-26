package net.wti.gradle.publish.task;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 1/26/19 @ 4:15 AM.
 */
public class XapiPublish extends DefaultTask {

    public static final String LIFECYCLE_TASK = "xapiPublish";

    @TaskAction
    public void publish() {
        // this task will be used as a lifecycle task,
        // but we're also going to have some input and output files,
        // so we can participate sanely in work avoidance.

    }

}
