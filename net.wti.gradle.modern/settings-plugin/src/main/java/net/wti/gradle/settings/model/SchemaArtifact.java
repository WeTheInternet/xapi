package net.wti.gradle.settings.model;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 2020-02-08 @ 5:16 a.m.
 */
public class SchemaArtifact {
    private final String groupId;
    private final String artifactId;
    private final String version;

    public SchemaArtifact(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "SchemaArtifact{" +
                "groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}

