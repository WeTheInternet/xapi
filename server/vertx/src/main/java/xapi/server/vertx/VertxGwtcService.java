package xapi.server.vertx;

import org.eclipse.aether.resolution.ArtifactResult;
import xapi.annotation.compile.Dependency;
import xapi.annotation.inject.InstanceOverride;
import xapi.dev.gwtc.api.GwtcService;
import xapi.dev.gwtc.impl.GwtcServiceImpl;
import xapi.fu.Lazy;
import xapi.fu.MappedIterable;
import xapi.fu.Out1;
import xapi.gwtc.api.Gwtc;
import xapi.gwtc.api.GwtcProperties;
import xapi.gwtc.api.ObfuscationLevel;
import xapi.mvn.X_Maven;
import xapi.time.X_Time;

import java.util.List;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/26/16.
 */
@Gwtc(
    propertiesLaunch = @GwtcProperties(
        obfuscationLevel = ObfuscationLevel.PRETTY,
        warDir = "./target/runtime"
    )
)
@InstanceOverride(implFor = GwtcService.class, priority = 2)
public class VertxGwtcService extends GwtcServiceImpl {

    @Override
    protected Out1<Iterable<String>> downloadFromMaven(Dependency dependency) {
        final Lazy<List<String>> artifact = Lazy.deferred1(()->{
            final ArtifactResult result = X_Maven.loadArtifact(
                dependency.groupId(),
                dependency.value(),
                dependency.classifier(),
                dependency.version()
            );
            return X_Maven.loadDependencies(result.getArtifact(), check->
                !"test".equals(check.getScope()) && !"system".equals(check.getScope())
            );
        });
        // Start download of artifact info immediately, but do not block
        X_Time.runLater(artifact.ignoreOut1().toRunnable());
        // Return a string output that will block on the lazy initializer
        return artifact.map(MappedIterable::mapped);
    }
}
