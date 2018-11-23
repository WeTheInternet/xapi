package xapi.gradle.task;

import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;

import java.util.Set;

/**
 * This task is responsible for syncing a set of artifacts into a build-local repository.
 *
 * This can be used to prime your local filesystem with all necessary artifacts,
 * so you can remove all runtime repositories (and network latency) from all builds.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 11/22/18 @ 10:49 PM.
 */
public class XapiPreload extends SourceTask {

    @TaskAction
    public void preload() {

    }

    public void addArtifacts(Iterable<ResolvedArtifact> artifacts) {

    }
}
