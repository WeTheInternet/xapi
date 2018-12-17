package xapi.gradle.task;

import org.gradle.api.artifacts.LenientConfiguration;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;
import xapi.fu.In1Out1;
import xapi.fu.Lazy;
import xapi.fu.Out1;
import xapi.fu.data.MapLike;
import xapi.fu.java.X_Jdk;
import xapi.util.X_String;

import java.util.*;

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

    public void addArtifacts(LenientConfiguration config) {
        addArtifacts(config.getAllModuleDependencies(), config::getArtifacts);
    }

    public void addArtifacts(Set<ResolvedDependency> allDeps, Out1<Set<ResolvedArtifact>> artifacts) {
        final MapLike<String, PreloadItem> items = X_Jdk.mapOrderedInsertion();
        final MapLike<String, ResolvedArtifact> resolved = X_Jdk.mapOrderedInsertion();
        Out1<MapLike<String, ResolvedArtifact>> resolve = Lazy.deferred1(artifacts.map(all->
            resolved.putFromValuesItr(all, this::artifactKey)
        ));
        for (ResolvedDependency item : allDeps) {
            final String config = item.getConfiguration();
            // TODO: actually handle classifier, or remove it from key calculations,
            // for now, we'll just multiplex the guts of PreloadItem.
            String key = toKey(item.getModule().getId(), null, "jar");
            items.compute(key, (k, was)->{
                if (was == null) {
                    return new PreloadItem(item, resolve.map(MapLike::get, key));
                } else {
                    was.addItem(item, resolve.map(MapLike::get, key));
                    return was;
                }
            });
        }
        // Now, for each preload item, we'll want to make a copy-and install task, then depend on it.
        // ...Ideally need to consider poms and file hashes here as well...
        // May actually just make sense to directory-sync the location containing the artifact's output.


    }

    private String artifactKey(ResolvedArtifact o) {
        return toKey(o.getModuleVersion().getId(), o.getClassifier(), o.getType());
    }

    private String toKey(ModuleVersionIdentifier id, String classifier, String type) {
        final String base = id.getGroup() + ":" + id.getName();
        final String suffix = X_String.isEmptyTrimmed(type) || "jar".equals(type) ? "" : "@" + type;
        return X_String.isEmptyTrimmed(classifier) ? base + suffix : base + ":" + classifier + suffix;
    }
}
