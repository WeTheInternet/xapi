package net.wti.gradle.schema.api;

import net.wti.gradle.api.MinimalProjectView;
import net.wti.gradle.require.api.PlatformModule;
import org.gradle.util.GFileUtils;
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
public class SchemaIndexReader implements SchemaDirs {

    private final CharSequence version;
    private SchemaPatternResolver properties;

    public boolean isMultiPlatform(final MinimalProjectView view, final String path, final PlatformModule coords) {
        File pathDir = new File(getDirIndex(), "path");
        File projectDir = new File(pathDir, QualifiedModule.mangleProjectPath(path));
        if ("_".equals(projectDir.getName())) {
            projectDir = new File(pathDir, "_" + view.getBuildName());
        }
        // if the parent project-wide directory is marked as multiplatform=true, we treat it as multiplatform
        File multiplatformFile;
        multiplatformFile = new File(projectDir, "multiplatform");
        if (multiplatformFile.exists() && "true".equals(GFileUtils.readFile(multiplatformFile).trim())) {
            return true;
        }
        // also allow the modules themselves to specify "I am multiplatform"
        final String key = PlatformModule.unparse(coords);
        File moduleDir = new File(projectDir, key);
        multiplatformFile = new File(moduleDir, "multiplatform");
        if (multiplatformFile.exists() && "true".equals(GFileUtils.readFile(multiplatformFile).trim())) {
            return true;
        }
        return false;
    }

    public boolean dependencyExists(final SchemaDependency dependency, final SchemaProject project, final SchemaPlatform plat, final SchemaModule mod) {
        final File dir = calcDependencyProjectDir(dependency, project, plat, mod);
        final File live = new File(dir, "live");
        return live.exists() && !"0".equals(GFileUtils.readFile(live, "utf-8"));
    }

    @Override
    public File getDirIndex() {
        return new File(indexDir);
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

    public SchemaIndexReader(final String indexDir, final CharSequence version, SchemaPatternResolver patterns) {
        this.version = version;
        this.indexDir = indexDir;
        this.indexResults = X_Jdk.mapHashConcurrent();
        this.properties = patterns;
    }

    public boolean hasEntries(final MinimalProjectView view, String projectName, SchemaPlatform platform, SchemaModule module) {
        if (getEntries(view, projectName, platform, module).isNotEmpty()) {
            return true;
        }
        String projectPath = mangleProjectPath(projectName);
        File pathDir = new File(indexDir, "path");
        File projectDir = new File(pathDir, projectPath);
        final String platMod = PlatformModule.unparse(platform.getName(), module.getName());
        File moduleDir;
        moduleDir = new File(projectDir, platMod);
        // hm... we may want to get more picky, like "check for sources file", "check for explicit 'create this' flag", etc.
//        return moduleDir.isDirectory() && Objects.requireNonNull(moduleDir.list()).length > 0;
        File liveFile = new File(moduleDir, "live");
        if (liveFile.exists() && !"0".equals(GFileUtils.readFile(liveFile, "utf-8"))) {
            return true;
        }
        return false;
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
        final String version = this.version.toString();
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

}
