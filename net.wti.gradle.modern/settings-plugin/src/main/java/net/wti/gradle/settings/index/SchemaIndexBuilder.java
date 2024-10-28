package net.wti.gradle.settings.index;

import net.wti.gradle.api.MinimalProjectView;
import net.wti.gradle.settings.api.*;
import xapi.fu.In1;
import xapi.fu.Lazy;
import xapi.fu.data.SetLike;
import xapi.fu.java.X_Jdk;
import xapi.gradle.fu.LazyString;

import java.io.File;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static net.wti.gradle.tools.GradleFiles.writeFile;

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
    private final SchemaIndexReader reader;
    private final IndexNodePool nodes;

    private String buildName;
    private CharSequence groupId;
    private CharSequence version;

    private SetLike<SchemaLiveness> allLiveModules = X_Jdk.setLinkedSynchronized();

    public SchemaIndex build() {
        final SchemaIndexImmutable immutable = new SchemaIndexImmutable(
                getBuildName(), getGroupIdNotNull(), version == null ? null : version.toString(),
                reader); // consider resetting the reader?
        return immutable;
    }

    public SchemaIndexBuilder duplicate() {
        SchemaIndexBuilder child = new SchemaIndexBuilder(view, nodes, rootDir, properties, executor, futures, indexDir);
        child.buildName = buildName;
        child.groupId = LazyString.nullableString(this::getGroupId);
        child.version = LazyString.nullableString(this::getVersion);
        return child;
    }

    public SchemaIndexBuilder(MinimalProjectView view, final IndexNodePool nodes, File rootDir, final SchemaProperties properties, final ExecutorService exec, final List<Future<?>> futures, final File indexDir) {
        this.view = view;
        this.nodes = nodes;
        this.rootDir = rootDir;
        this.buildName = rootDir.getName();
        this.properties = properties;
        this.executor = exec;
        this.futures = futures;
        this.indexDir = indexDir;
        this.reader = properties.createReader(view, nodes, new LazyString(this::getVersion));
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

    public IndexNodePool getNodes() {
        return nodes;
    }

    public void markWithSource(final SchemaProject project, final SchemaPlatform plat, final SchemaModule mod, final File moduleSrc) {
        final SchemaLiveness liveness = new SchemaLiveness(project, plat, mod);
        allLiveModules.add(liveness);
        liveness.setModuleSrc(moduleSrc);
    }

    public void markExplicitInclude(final SchemaProject project, final SchemaPlatform plat, final SchemaModule mod) {
        final SchemaLiveness liveness = new SchemaLiveness(project, plat, mod);
        allLiveModules.addAndReturn(liveness).setExplicit(true);
    }

    public void forAllLiveModules(In1<SchemaLiveness> callback) {
        allLiveModules.forAll(callback);
    }

    public void recordProject(final SchemaIndexBuilder index, final MinimalProjectView view, final SchemaProject project) {
        final File projectDir = index.calcProjectDir(project);
        if (project.isMultiplatform()) {
            File multiplatform = new File(projectDir, "multiplatform");
            writeFile(multiplatform, "true");
        } else {
            // we should really validate that there's only one live platform/module.
            // ....later
        }

    }

    public void compressNodes(final IndexNodePool nodes) {
        // we should have already pruned any leaf nodes that ultimately depend on nothing.
        // now that we've processed the whole graph at least once, we're now ready to
        // also prune anything which has includes, but no other liveness reasons.
        for (IndexNode node : nodes.getAllNodes()) {
            final SetLike<IndexNode> dependencies = node.getCompressedDependencies();
            final EnumSet<LivenessReason> reasons = node.getLivenessReasons();
            if (reasons.isEmpty()) {
                // this node should be erased
                nodes.delete(node.getIdentity());
            } else if (reasons.size() == 1) {
                if (reasons.contains(LivenessReason.has_includes)) {
                    if (dependencies.isEmpty()) {
                        // a module that only includes, but has nothing it depends on is worthless
                        nodes.delete(node.getIdentity());
                    } else if (!node.hasOutgoing()) {
                        // a module that only includes, but nothing that is live depends on, should be deleted
                        nodes.delete(node.getIdentity());
                    }
                }
            }
        }

    }

    public class SchemaLiveness {
        private final SchemaProject project;
        private final SchemaPlatform platform;
        private final SchemaModule module;
        private final String key;
        private File moduleSrc;
        private boolean explicit;

        public SchemaLiveness(final SchemaProject project, final SchemaPlatform platform, final SchemaModule module) {
            this.project = project;
            this.platform = platform;
            this.module = module;
            final String projName = QualifiedModule.mangleProjectPath(project.getPathGradle());
            final String platMod = QualifiedModule.unparse(platform.getName(), module.getName());
            this.key = projName + "_" + platMod;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final SchemaLiveness that = (SchemaLiveness) o;
            return key.equals(that.key);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key);
        }

        public String getKey() {
            return key;
        }

        public SchemaProject getProject() {
            return project;
        }

        public SchemaPlatform getPlatform() {
            return platform;
        }

        public SchemaModule getModule() {
            return module;
        }

        public File getModuleSrc() {
            return moduleSrc;
        }

        public void setModuleSrc(final File moduleSrc) {
            this.moduleSrc = moduleSrc;
        }

        public File getProjectDir() {
            return calcProjectDir(project, platform.getName(), module.getName());
        }

        public boolean isExplicit() {
            return explicit;
        }

        public void setExplicit(final boolean explicit) {
            this.explicit = explicit;
        }

        public boolean isMultiplatform() {
            return project.isMultiplatform();
        }

        public boolean isVirtual() {
            return project.isVirtual();
        }

        public boolean isForce() {
            return project.isForce();
        }

    }

    @Override
    public SchemaIndexReader getReader() {
        return reader;
    }
}

