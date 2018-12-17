package xapi.gradle.task;

import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedDependency;
import xapi.fu.Lazy;
import xapi.fu.Out1;

/**
 * Assembled metadata about a dependency we are preloading into local cache.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 12/16/18 @ 10:38 PM.
 */
public class PreloadItem {

    private final ResolvedDependency dependency;
    private final Lazy<ResolvedArtifact> artifact;

    public PreloadItem(ResolvedDependency dep, Out1<ResolvedArtifact> artifact) {
        this.dependency = dep;
        this.artifact = Lazy.deferred1(artifact);
    }
    public PreloadItem addItem(ResolvedDependency dep, Out1<ResolvedArtifact> artifact) {
        return this;
    }
}
