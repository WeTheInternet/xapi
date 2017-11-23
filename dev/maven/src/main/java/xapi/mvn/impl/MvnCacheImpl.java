package xapi.mvn.impl;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.resolution.ArtifactResult;
import xapi.annotation.inject.InstanceDefault;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.fu.Lazy;
import xapi.fu.Out1;
import xapi.mvn.api.MvnCoords;
import xapi.mvn.api.MvnModule;
import xapi.mvn.api.MvnCache;
import xapi.mvn.service.MvnService;
import xapi.util.X_String;

import java.io.File;

/**
 * In cases where we need to lookup values from jars and poms, we do not want to have to keep resolving artifacts,
 * and inspecting poms; so, we use this cache to help speed up repeated lookups.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 9/4/16.
 */
@InstanceDefault(implFor = MvnCache.class)
public class MvnCacheImpl implements MvnCache {

    private final MvnService service;
    private final StringTo<Lazy<ArtifactResult>> resultCache = X_Collect.newStringMap(Lazy.class, X_Collect.MUTABLE_CONCURRENT);
    private final StringTo<Model> modelCache = X_Collect.newStringMap(Model.class, X_Collect.MUTABLE_CONCURRENT);

    public MvnCacheImpl(MvnService service) {
        this.service = service;
    }

    public Model getPom(Parent artifact) {
        return getPom(artifact.getGroupId(), artifact.getArtifactId(), "pom", "", artifact.getVersion());
    }

    public Model getPom(MvnCoords<?> artifact) {
        return getPom(artifact.getGroupId(), artifact.getArtifactId(), "pom", "", artifact.getVersion());
    }

    public Model getPom(String groupId, String artifactId, String extension, String classifier, String version) {
        final Artifact artifact = getArtifact(groupId, artifactId, classifier, extension, version);
        return getPom(artifact);
    }

    public Model getPom(Artifact artifact) {
        String fileName = artifact.getFile().getAbsolutePath();
        return modelCache.getOrCreateUnsafe(fileName, service::loadPomFile);
    }

    public Model getPom(Model model, Dependency artifact) {
        String groupId = artifact.getGroupId();
        String artifactId = artifact.getArtifactId();
        String extension = artifact.getType();
        String classifier = artifact.getClassifier();
        String version = artifact.getVersion();
        if (version.startsWith("$")) {
            version = getProperty(model, version);
        }
        if (groupId.startsWith("$")) {
            groupId = getProperty(model, groupId);
        }
        if (artifactId.startsWith("$")) {
            artifactId = getProperty(model, artifactId);
        }
        if (extension == null) {
            extension = "jar";
        } else if (extension.startsWith("$")) {
            extension = getProperty(model, extension);
        }
        if (classifier == null) {
            classifier = "";
        } else if (classifier.startsWith("$")) {
            classifier = getProperty(model, classifier);
        }
        return getPom(groupId, artifactId, extension, classifier, version);
    }

    public String getGroupId(Model model) {
        return resolveProperty(model, "${pom.groupId}");
    }

    public String resolveProperty(Model model, String property, String dflt) {
        String result = resolveProperty(model, property);
        return result == null ? dflt : result;
    }

    public String resolveProperty(Model model, String property) {
        if (property == null) {
            return null;
        }
        if (property.startsWith("$")) {
            return getProperty(model, property);
        }
        return property;
    }

    public String getProperty(Model model, String propertyName, Out1<String> dflt) {
        String value = getProperty(model, propertyName);
        if (X_String.isEmpty(value) || value.equals(propertyName)) {
            return dflt.out1();
        }
        return value;
    }
    public String getProperty(Model model, String propertyName) {
        // remove ${wrapper}
        String before = "";
        String after = "";
        int start = propertyName.indexOf("${");
        if (start != -1) {
            if (start > 0) {
                before = propertyName.substring(0, start);
            }
            final int end = propertyName.indexOf('}', start);
            if (end < propertyName.length()) {
                after = propertyName.substring(end + 1);
            }
            propertyName = propertyName.substring(start + 2, end);
        }
        if ("project.build.directory".equals(propertyName) ) {
            return before + getBuildDirectory(model) + after;
        }
        if ("project.build.sourceDirectory".equals(propertyName) ) {
            return before + getSourceDirectory(model) + after;
        }
        if ("project.directory".equals(propertyName) || "basedir".equals(propertyName)) {
            return before + model.getProjectDirectory() + after;
        }
        if ("project.version".equals(propertyName) || "pom.version".equals(propertyName)) {
            if (model.getVersion() != null) {
                return before + model.getVersion() + after;
            } else if (model.getParent() != null) {
                return before + model.getParent().getVersion() + after;
            }
        }
        if ("pom.groupId".equals(propertyName) || "project.groupId".equals(propertyName)) {
            if (model.getGroupId() != null) {
                return before + model.getGroupId() + after;
            } else if (model.getParent() != null) {
                return before + model.getParent().getGroupId() + after;
            }
        }
        if ("pom.artifactId".equals(propertyName) || "project.groupId".equals(propertyName)) {
            return before + model.getArtifactId() + after;
        }
        if (model.getProperties() != null) {
            String value = model.getProperties().getProperty(propertyName);
            if (value != null) {
                return before + value + after;
            }
        }
        if (model.getParent() != null) {
            return before + getProperty(getPom(model.getParent()), propertyName) + after;
        }
        String result = service.normalize(propertyName);
        if (result.startsWith("$")) {
            throw new IllegalStateException("Model " + model + " does not contain property " + propertyName);
        }
        return before + result + after;
    }

    private String getBuildDirectory(Model model) {
        if (model.getBuild() == null || model.getBuild().getDirectory() == null) {
            return new File(model.getProjectDirectory(), "target").getAbsolutePath();
        }
        return model.getBuild().getDirectory();
    }
    private String getSourceDirectory(Model model) {
        if (model.getBuild() == null || model.getBuild().getSourceDirectory() == null) {
            return new File(model.getProjectDirectory(), "src/main/java").getAbsolutePath();
        }
        return model.getBuild().getSourceDirectory();
    }

    public String toArtifactString(Model pom, Dependency artifact) {
        String groupId = resolveProperty(pom, artifact.getGroupId());
        String artifactId = resolveProperty(pom, artifact.getArtifactId());
        String version = resolveProperty(pom, artifact.getVersion());
        String type = resolveProperty(pom, artifact.getType());
        String classifier = resolveProperty(pom, artifact.getClassifier());
        if (type == null) {
            if (classifier == null) {
                return groupId + ":" + artifactId + ":" + version;
            } else {
                return groupId + ":" + artifactId + ":" + classifier + ":" + version;
            }
        } else {
            if (classifier == null) {
                return groupId + ":" + artifactId + ":" + type + ":" + version;
            } else {
                return groupId + ":" + artifactId + ":" + type + ":" + classifier + ":" + version;
            }
        }
    }

    public Artifact loadArtifact(Model pom, Dependency dependency) {
        if (dependency.getType() == null) {
            if (dependency.getClassifier() == null) {
                return loadArtifact(pom,
                    dependency.getGroupId(),
                    dependency.getArtifactId(),
                    "",
                    "jar",
                    dependency.getVersion()
                );
            } else {
                return loadArtifact(
                    pom, dependency.getGroupId(),
                    dependency.getArtifactId(),
                    dependency.getClassifier(),
                    "jar",
                    dependency.getVersion()
                );
            }
        } else {
            if (dependency.getClassifier() == null) {
                return loadArtifact(
                    pom, dependency.getGroupId(),
                    dependency.getArtifactId(),
                    "",
                    dependency.getType(),
                    dependency.getVersion()
                );
            } else {
                return loadArtifact(
                    pom, dependency.getGroupId(),
                    dependency.getArtifactId(),
                    dependency.getClassifier(),
                    dependency.getType(),
                    dependency.getVersion()
                );

            }
        }
    }

    public Artifact loadArtifact(
        Model pom,
        String groupId,
        String artifactId,
        String classifier,
        String type,
        String version
    ) {
        groupId = resolveProperty(pom, groupId);
        artifactId = resolveProperty(pom, artifactId);
        classifier = resolveProperty(pom, classifier, "");
        type = resolveProperty(pom, type, "jar");
        version = resolveProperty(pom, version);

        return getArtifact(groupId, artifactId, classifier, type, version);
    }

    private Artifact getArtifact(String groupId, String artifactId, String classifier, String type, String version) {
        String key = groupId+"|"+artifactId+"|"+type+"|"+classifier+"|"+version;

        final Lazy<ArtifactResult> result =
            resultCache.getOrCreate(key, k->
                // we want to reduce co-mod in cache's .getOrCreate, so we return a placeholder Lazy
                Lazy.deferred1(()->service.loadArtifact(
                    groupId,
                    artifactId,
                    classifier,
                    type,
                    version
                )
            ));

        // Race conditions will block on the first entrant here
        return result.out1().getArtifact();
    }

    @Override
    public MvnModule getModule(MvnCoords<?> coords) {
        final Model pom = getPom(coords);

        return null;
    }
}
