package net.wti.gradle.schema.index;

import net.wti.gradle.api.MinimalProjectView;
import net.wti.gradle.schema.spi.SchemaIndex;
import xapi.gradle.fu.LazyString;

import java.io.File;

/**
 * Responsible for building a SchemaIndex; basically just exposes the setters of a SchemaIndex
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 2020-07-20 @ 2:50 a.m..
 */
public class SchemaIndexBuilder implements SchemaIndex {

    private final MinimalProjectView view;
    private final File rootDir;

    private String buildName;
    private CharSequence groupId;
    private CharSequence version;

    public SchemaIndex build() {
        final SchemaIndexImmutable immutable = new SchemaIndexImmutable(getBuildName(), getGroupIdNotNull(), version.toString());
        return immutable;
    }

    public SchemaIndexBuilder duplicate() {
        SchemaIndexBuilder child = new SchemaIndexBuilder(view, rootDir);
        child.buildName = buildName;
        child.groupId = LazyString.nullableString(this::getGroupId);
        child.version = LazyString.nullableString(this::getVersion);
        return child;
    }

    public SchemaIndexBuilder(MinimalProjectView view, File rootDir) {
        this.view = view;
        this.rootDir = rootDir;
        this.buildName = rootDir.getName();
    }

    @Override
    public String getBuildName() {
        return buildName;
    }

    public void setBuildName(String buildName) {
        this.buildName = buildName;
    }

    @Override
    public CharSequence getGroupId() {
        return groupId;
    }

    public SchemaIndexBuilder setGroupId(CharSequence groupId) {
        this.groupId = groupId;
        return this;
    }

    @Override
    public CharSequence getVersion() {
        return version;
    }

    public SchemaIndexBuilder setVersion(CharSequence version) {
        this.version = version;
        return this;
    }
}
