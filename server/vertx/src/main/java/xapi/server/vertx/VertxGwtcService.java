package xapi.server.vertx;

import xapi.annotation.compile.Dependency;
import xapi.annotation.inject.InstanceOverride;
import xapi.dev.gwtc.api.GwtcService;
import xapi.dev.gwtc.impl.GwtcServiceImpl;
import xapi.fu.Out1;
import xapi.fu.X_Fu;
import xapi.gwtc.api.Gwtc;
import xapi.gwtc.api.GwtcProperties;
import xapi.gwtc.api.ObfuscationLevel;
import xapi.model.X_Model;
import xapi.mvn.X_Maven;
import xapi.mvn.api.MvnDependency;

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
        MvnDependency dep = X_Model.create(MvnDependency.class);
        dep.setGroupId(dependency.groupId());
        dep.setArtifactId(dependency.value());
        dep.setVersion(dependency.version());
        dep.setClassifier(dependency.classifier());
        if (dependency.specifiers().length > 0) {
            dep.setPackaging(dependency.specifiers()[0].type());
        }
        return X_Maven.downloadDependencies(dep).map(X_Fu::downcast);
    }
}
