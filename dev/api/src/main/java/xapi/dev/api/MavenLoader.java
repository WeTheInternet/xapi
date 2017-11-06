package xapi.dev.api;

import xapi.annotation.compile.Dependency;
import xapi.fu.Out1;
import xapi.model.X_Model;
import xapi.mvn.api.MvnDependency;
import xapi.util.X_Namespace;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/4/17.
 */
@FunctionalInterface // knock yourself out ;-)
public interface MavenLoader {

    // TODO make less onerous by making the method async
    Out1<Iterable<String>> downloadDependency(MvnDependency dependency);

    default MvnDependency getDependency(Dependency dependency) {
        return getDependency(dependency.groupId(), dependency.value(),
            // This is hideous... lets fix it...
            dependency.specifiers() == null || dependency.specifiers().length == 0 ? "jar" : dependency.specifiers()[0].type(),
            dependency.classifier(), dependency.version());
    }

    default MvnDependency getDependency(String artifactId) {
        return getDependency(X_Namespace.XAPI_GROUP_ID, artifactId, "jar", null, X_Namespace.XAPI_VERSION);
    }

    default MvnDependency getDependency(String groupId, String artifactId, String version) {
        return getDependency(groupId, artifactId, "jar", null, version);
    }

    default MvnDependency getDependency(String groupId, String artifactId, String type, String classifier, String version) {
        final MvnDependency dep = X_Model.create(MvnDependency.class);
        dep.setGroupId(groupId);
        dep.setArtifactId(artifactId);
        dep.setVersion(version);
        if (classifier != null) {
            if (type == null) {
                dep.setPackaging("jar");
            }
            dep.setClassifier(classifier);
        }
        if (type != null) {
            dep.setPackaging(type);
        }
        return dep;
    }
}
