package net.wti.gradle.schema.index;

import net.wti.gradle.api.MinimalProjectView;
import net.wti.gradle.require.api.PlatformModule;
import net.wti.gradle.schema.api.QualifiedModule;
import net.wti.gradle.schema.api.SchemaModule;
import net.wti.gradle.schema.api.SchemaPlatform;
import net.wti.gradle.schema.map.SchemaMap;
import net.wti.gradle.schema.spi.SchemaIndexer;
import net.wti.gradle.schema.spi.SchemaProperties;
import xapi.fu.data.MapLike;
import xapi.fu.java.X_Jdk;

import java.io.File;
import java.util.*;

import static net.wti.gradle.schema.api.QualifiedModule.mangleProjectPath;

/**
 * SchemaIndexReader:
 * <p>
 * <p> This class is responsible for reading the on-disk metadata of the xapi index,
 * <p> and exposing it in an easy-to-query format, backed by in-memory cache
 * <p> (if you expect the cache to change while you are running, for now, call clear()
 * <p> method, and if you feel strongly, feel free to implement file / directory watches).
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 16/03/2021 @ 1:39 a.m..
 */
public class SchemaIndexReader {

    private final SchemaMap map;
    private SchemaProperties properties;

    public boolean isMultiPlatform(final MinimalProjectView view, final String path) {
        return false;
    }

    public static class IndexResult implements Iterable<File> {
        private final List<File> discoveredFiles;
        private final boolean found;

        public IndexResult(final List<File> discoveredFiles) {
            this.found = discoveredFiles != null && !discoveredFiles.isEmpty();
            this.discoveredFiles = found ? discoveredFiles : Collections.emptyList();
        }

        public boolean isNotEmpty() {
            return found;
        }

        public boolean isEmpty() {
            return !found;
        }

        @Override
        public ListIterator<File> iterator() {
            return discoveredFiles.listIterator();
        }
    }

    private final String indexDir;
    private final MapLike<String, IndexResult> indexResults;

    public SchemaIndexReader(SchemaProperties props, MinimalProjectView view, SchemaMap map) {
        // consider gradle properties in addition to env vars and sys props.
        this(props == null || props == SchemaProperties.getInstance() ? SchemaIndexer.getIndexLocation(view) : props.getIndexLocation(view), map);
        setProperties(props);
    }

    public SchemaIndexReader(final String indexDir, final SchemaMap map) {
        this.map = map;
        this.indexDir = indexDir;
        this.indexResults = X_Jdk.mapHashConcurrent();
    }

    public boolean hasEntries(final MinimalProjectView view, String projectName, SchemaPlatform platform, SchemaModule module) {
        if (getEntries(view, projectName, platform, module).isNotEmpty()) {
            return true;
        }
        String projectPath = mangleProjectPath(projectName);
        File pathDir = new File(indexDir, "path");
        File projectDir = new File(pathDir, projectPath);
        final String platMod = PlatformModule.unparse(platform.getName(), module.getName());
        File moduleDir = new File(projectDir, platMod);
        moduleDir = new File(projectDir, platMod);
        // hm... we may want to get more picky, like "check for sources file", "check for explicit 'create this' flag", etc.
        return moduleDir.isDirectory() && Objects.requireNonNull(moduleDir.list()).length > 0;
    }

    public IndexResult getEntries(MinimalProjectView view, String projectName, SchemaPlatform platform, SchemaModule module) {
        String key = toKey(projectName, platform.getName(), module.getName());
        return indexResults.computeIfAbsent(key, ()->readIndex(view, projectName, platform, module));
    }

    private IndexResult readIndex(final MinimalProjectView view, final String projectName, final SchemaPlatform platform, final SchemaModule module) {
        File indexRoot = new File(indexDir);

        final String groupPattern = platform.getPublishPattern();
        final String namePattern = module.getPublishPattern();
        final String group = properties.resolvePattern(groupPattern, view.getBuildName(), projectName, view.getGroup(), view.getVersion(), platform.getName(), module.getName());
        final String name = properties.resolvePattern(namePattern, view.getBuildName(), projectName, view.getGroup(), view.getVersion(), platform.getName(), module.getName());
        final String version = map.getVersion();
        final List<File> files = new ArrayList<>();

        File coord = new File(indexRoot, "coord");
        File groupDir = new File(coord, group);
        File moduleDir = new File(groupDir, name);
        File versionDir = new File(moduleDir, version);
        if (versionDir.isDirectory()) {
            for (File dep : versionDir.listFiles()) {
                if (dep.isDirectory()) {
                    files.addAll(Arrays.asList(dep.listFiles()));
                } else {
                    files.add(dep);
                }
            }
        }
        // hm
        final IndexResult result = new IndexResult(Collections.unmodifiableList(files));
        return result;
    }

    private String toKey(final String projectName, final String platform, final String module) {
        if (projectName == null || projectName.isEmpty() || ":".equals(projectName)) {
            return platform + " " + module;
        }
        return projectName + " " + platform + " " + module;
    }

    public SchemaIndexReader setProperties(final SchemaProperties properties) {
        this.properties = properties;
        return this;
    }

    public SchemaProperties getProperties() {
        return properties;
    }

}
