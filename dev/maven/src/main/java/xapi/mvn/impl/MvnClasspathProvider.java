package xapi.mvn.impl;

import xapi.dev.api.AbstractClasspathProvider;
import xapi.fu.Lazy;
import xapi.fu.MappedIterable;
import xapi.fu.Out1;
import xapi.model.X_Model;
import xapi.mvn.X_Maven;
import xapi.mvn.api.MvnDependency;
import xapi.mvn.service.MvnService;
import xapi.scope.X_Scope;
import xapi.scope.api.GlobalScope;
import xapi.scope.api.Scope;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/11/17.
 */
public abstract class MvnClasspathProvider <Self extends MvnClasspathProvider<Self>> extends AbstractClasspathProvider<Self> {

    private final Lazy<MvnService> service = Lazy.deferred1(()->
        X_Scope.currentScopeIfNull(scope)
            .getOrCreateIn(mvnScope(), MvnService.class, ignored-> X_Maven.getMavenService())
    );

    private Class<? extends Scope> mvnScope() {
        return GlobalScope.class;
    }

    public Out1<MappedIterable<String>> fromMaven(String groupdId, String artifactId, String packaging, String classifier, String version) {
        final MvnDependency dep = dependency();
        if (groupdId != null) {
            // in case you overrode dependency() to supply your own defaults, we'll be defensive...
            dep.setGroupId(groupdId);
        }
        assert artifactId != null : "You really must at least specify an artifact id";
        dep.setArtifactId(artifactId);
        if (version != null) {
            dep.setVersion(version);
        }
        if (classifier != null) {
            dep.setClassifier(classifier);
            dep.setPackaging(packaging == null ? "jar" : classifier);
        } else if (packaging != null) {
            dep.setPackaging(packaging);
        }
        return service.out1().downloadDependencies(dep);

    }
    public Out1<MappedIterable<String>> fromMaven(String groupdId, String artifactId, String version) {
        return fromMaven(groupdId, artifactId, null, null, version);
    }

    public Out1<MappedIterable<String>> fromMaven(String groupdId, String artifactId, String packaging, String version) {
        return fromMaven(groupdId, artifactId, packaging, null, version);
    }

    protected MvnDependency dependency() {
        return X_Model.create(MvnDependency.class);
    }

}
