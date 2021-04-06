package net.wti.gradle.schema.index;

import net.wti.gradle.api.MinimalProjectView;
import net.wti.gradle.schema.api.*;
import net.wti.gradle.schema.spi.SchemaIndex;
import net.wti.gradle.schema.spi.SchemaProperties;
import xapi.fu.Lazy;
import xapi.gradle.fu.LazyString;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static net.wti.gradle.schema.api.QualifiedModule.mangleProjectPath;

/**
 * Responsible for building a SchemaIndex; basically just exposes the setters of a SchemaIndex
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 2020-07-20 @ 2:50 a.m..
 */
public class SchemaIndexBuilder implements SchemaIndex, SchemaDirs {

    private final MinimalProjectView view;
    private final File rootDir;
    private final SchemaProperties properties;
    private final ExecutorService executor;
    private final List<Future<?>> futures;
    private final File indexDir;

    private String buildName;
    private CharSequence groupId;
    private CharSequence version;

    public SchemaIndex build() {
        final SchemaIndexImmutable immutable = new SchemaIndexImmutable(getBuildName(), getGroupIdNotNull(), version.toString());
        return immutable;
    }

    public SchemaIndexBuilder duplicate() {
        SchemaIndexBuilder child = new SchemaIndexBuilder(view, rootDir, properties, executor, futures, indexDir);
        child.buildName = buildName;
        child.groupId = LazyString.nullableString(this::getGroupId);
        child.version = LazyString.nullableString(this::getVersion);
        return child;
    }

    public SchemaIndexBuilder(MinimalProjectView view, File rootDir, final SchemaProperties properties, final ExecutorService exec, final List<Future<?>> futures, final File indexDir) {
        this.view = view;
        this.rootDir = rootDir;
        this.buildName = rootDir.getName();
        this.properties = properties;
        this.executor = exec;
        this.futures = futures;
        this.indexDir = indexDir;
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

    public SchemaProperties getProperties() {
        return properties;
    }

    public File getRootDir() {
        return rootDir;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public List<Future<?>> getFutures() {
        return futures;
    }

    @Override
    public File getDirIndex() {
        return indexDir;
    }

    public File calcProjectDir(final SchemaProject project, CharSequence platform, CharSequence module) {
        final File projectDir = calcProjectDir(project);
        final File platformDir = new File(projectDir, platform.toString());
        //noinspection UnnecessaryLocalVariable (easier debugging return values)
        final File moduleDir = new File(platformDir, module.toString());
        return moduleDir;
    }

    public File calcProjectDir(final SchemaProject project) {
        final String path = project.getPathGradle();
        final File byPath = new File(getDirIndex(), "path"); // organized by gradle path $buildName:$project:$path
        //noinspection UnnecessaryLocalVariable (easier debugging return values)
        File projectDir = new File(byPath, mangleProjectPath(path));
        return projectDir;
    }

    public Lazy<File> getDirGNV(final String group, final String name, final String version) {
        File groupDir = new File(getDirByGnv(), group);
        File nameDir = new File(groupDir, name);
        File versionDir = new File(nameDir, version);
        return Lazy.deferred1(()-> {
            if (!versionDir.isDirectory()) {
                if (!versionDir.mkdirs()) {
                    if (!versionDir.isDirectory()) {
                        throw new IllegalStateException("Unable to create index publishing directory " + versionDir);
                    }
                }
            }
            return versionDir;
        });
    }

    public File calcDependencyProjectDir(final MinimalProjectView view, final SchemaDependency dep, final SchemaProject project, final SchemaPlatform platform, final SchemaModule module) {
        final String ppm = dep.getPPM(project.getPathGradle(), platform.getName(), module.getName());
        final File ppmDir = new File(getDirByPpm(), ppm.replace(':', File.separatorChar));
        return ppmDir;
    }

    public String calcPPM(final SchemaProject project, final SchemaPlatform platform, final SchemaModule module) {

        return null;
    }
}
