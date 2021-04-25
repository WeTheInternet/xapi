package net.wti.gradle.schema.index;

import net.wti.gradle.schema.api.QualifiedModule;
import net.wti.gradle.schema.api.SchemaIndex;
import net.wti.gradle.schema.api.SchemaIndexReader;

import java.util.Objects;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 2020-07-20 @ 2:18 a.m..
 */
public class SchemaIndexImmutable implements SchemaIndex {

    private final String buildName;
    private final String groupId;
    private final String version;
    private final SchemaIndexReader reader;

    public SchemaIndexImmutable(String buildName, CharSequence groupId, CharSequence version, final SchemaIndexReader reader) {
        this.buildName = buildName;
        this.groupId = groupId == null ? QualifiedModule.UNKNOWN_VALUE : groupId.toString();
        this.version = version == null ? QualifiedModule.UNKNOWN_VALUE : version.toString();
        this.reader = reader;
    }

    @Override
    public String getBuildName() {
        return buildName;
    }

    @Override
    public CharSequence getGroupId() {
        return groupId;
    }

    @Override
    public CharSequence getVersion() {
        return version;
    }

    @Override
    public SchemaIndexReader getReader() {
        return reader;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SchemaIndexImmutable that = (SchemaIndexImmutable) o;
        return buildName.equals(that.buildName) &&
                Objects.equals(groupId, that.groupId) &&
                Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(buildName, groupId);
    }
}
