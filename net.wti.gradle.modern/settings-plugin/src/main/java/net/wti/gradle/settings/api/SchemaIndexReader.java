package net.wti.gradle.settings.api;

import net.wti.gradle.api.BuildCoordinates;
import net.wti.gradle.api.MinimalProjectView;
import net.wti.gradle.settings.index.IndexNodePool;
import xapi.fu.Lazy;
import xapi.fu.data.MapLike;
import xapi.fu.java.X_Jdk;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.unmodifiableList;
import static net.wti.gradle.settings.api.QualifiedModule.mangleProjectPath;
import static net.wti.gradle.tools.GradleFiles.readFile;

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
    private final IndexNodePool nodes;
    private SchemaPatternResolver properties;

    public boolean isMultiPlatform(final MinimalProjectView view, final String path, final PlatformModule coords) {
        File pathDir = new File(getDirIndex(), "path");
        File projectDir = new File(pathDir, QualifiedModule.mangleProjectPath(path));
        if ("_".equals(projectDir.getName())) {
            projectDir = new File(pathDir, "_" + view.getBuildName());
        }
        final File[] allFiles = projectDir.listFiles();
        if (allFiles != null && allFiles.length > 1) {
            if (allFiles.length != 2) {
                return true;
            }
            for (File file : allFiles) {
                switch(file.getName()) {
                    case "main":
                    case "test":
                        break;
                    default:
                        return true;
                }
            }
        }
        // if the parent project-wide directory is marked as multiplatform=true, we treat it as multiplatform
        File multiplatformFile;
        multiplatformFile = new File(projectDir, "multiplatform");
        if (multiplatformFile.exists() && "true".equals(readFile(multiplatformFile).trim())) {
            return true;
        }
        // also allow the modules themselves to specify "I am multiplatform"
        final String key = PlatformModule.unparse(coords);
        File moduleDir = new File(projectDir, key);
        multiplatformFile = new File(moduleDir, "multiplatform");
        if (multiplatformFile.exists() && "true".equals(readFile(multiplatformFile).trim())) {
            return true;
        }
        return false;
    }

    public boolean dependencyExists(final SchemaDependency dependency, final SchemaProject project, final SchemaPlatform plat, final SchemaModule mod) {
        final File dir = calcDependencyProjectDir(dependency, project, plat, mod);
        final File live = new File(dir, "live");
        return live.exists() && !"0".equals(readFile(live));
    }

    @Override
    public File getDirIndex() {
        return new File(indexDir);
    }

    public static class IndexResult {
        private final List<File> internal, external, project, unknown;
        private final boolean explicitDependencies;
        private final int liveness;

        public IndexResult(
                @Nonnull final List<File> internal,
                @Nonnull final List<File> external,
                @Nonnull final List<File> project,
                @Nonnull final List<File> unknown,
                final int liveness) {
            this.internal = internal;
            this.external = external;
            this.project = project;
            this.unknown = unknown;
            this.liveness = liveness;
            explicitDependencies = !external.isEmpty() && !project.isEmpty();
        }

        public int getLiveness() {
            return liveness;
        }

        public boolean hasExplicitDependencies() {
            return explicitDependencies;
        }

        public boolean isStillValid() {
            // TODO: have a mark-sweep file we read in that we can compare to
            return true;
        }
    }

    private final String indexDir;
    private final MapLike<String, IndexResult> indexResults;
    private final MapLike<String, Boolean> livenessCheck;


    public SchemaIndexReader(final IndexNodePool nodes, final String indexDir, final CharSequence version, SchemaPatternResolver patterns) {
        this.nodes = nodes;
        this.version = version;
        this.indexDir = indexDir;
        this.indexResults = X_Jdk.mapHashConcurrent();
        this.livenessCheck = X_Jdk.mapHashConcurrent();
        this.properties = patterns;
    }

    public boolean hasEntries(final BuildCoordinates coords, String projectName, SchemaPlatform platform, SchemaModule module) {
        final IndexResult result = getEntries(coords, projectName, platform, module);
        if (result.hasExplicitDependencies()) {
            // any module w/ discovered explicit dependencies is automatically live, even if only to serve as transitive dependency node
            System.out.println(coords + " in " + projectName + " " + platform + ":" + module + " is live because it has explicit dependencies");
            return true;
        }
        // without explicit dependencies, we need to check for a liveness level > 2
        String projectPath = mangleProjectPath(projectName);
        final String platMod = PlatformModule.unparse(platform.getName(), module.getName());
        return checkLive(projectPath, platMod);
    }

    public boolean checkLive(final String projectPath, final String platMod) {
        final String key = projectPath + "@" + platMod;
        return livenessCheck.computeIfAbsent(key, ()-> {
            File pathDir = new File(indexDir, "path");
            File projectDir = new File(pathDir, projectPath);
            final File moduleDir = new File(projectDir, platMod);
            // hm... we may want to get more picky, like "check for sources file", "check for explicit 'create this' flag", etc.
            File liveFile = new File(moduleDir, "live");
            String liveValue = "0";
            if (liveFile.exists()) {
                liveValue = readFile(liveFile);
            }
            if ("0".equals(liveValue)) {
                // definitely ignore
                return false;
            }
            if ("1".equals(liveValue)) {
                // check for in links
                File inDir = new File(moduleDir, "in");
                if (!inDir.exists()) {
                    return false;
                }
                final File[] ins = inDir.listFiles();
                if (ins == null) {
                    return false;
                }
                File outDir = new File(moduleDir, "out");
                if (!outDir.exists()) {
                    return false;
                }
                final File[] outs = outDir.listFiles();
                if (outs == null) {
                    return false;
                }
                return checkIfDirHasLiveness(outs) && checkIfDirHasLiveness(ins);
            } else {
                // 2 or greater: definitely live
                return true;
            }
        });
    }

    private boolean checkIfDirHasLiveness(final File[] ins) {
        for (File projIn : ins) {
            final File[] platModIn = projIn.listFiles();
            if (platModIn == null) {
                // empty? should perhaps raise an error
                continue;
            }
            for (File platModDir : platModIn) {
                File link = new File(platModDir, "link");
                if (link.exists()) {
                    // if the target of this link is live, then so are we.
                    final String targetPlatmod = platModDir.getName();
                    final String targetProject = projIn.getName();
                    if (checkLive(targetProject, targetPlatmod)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public IndexResult getEntries(BuildCoordinates coords, String projectName, SchemaPlatform platform, SchemaModule module) {
        String key = toKey(projectName, platform.getName(), module.getName());
        return indexResults.computeValue(key, existing-> {
            if (existing != null && existing.isStillValid()) {
                return existing;
            }
            return readIndex(coords, projectName, platform, module);
        });

    }

    private IndexResult readIndex(final BuildCoordinates coords, final String projectName, final SchemaPlatform platform, final SchemaModule module) {
        File indexRoot = new File(indexDir);

        final String groupPattern = platform.getPublishPattern();
        final String namePattern = module.getPublishPattern();
        final String group = properties.resolvePattern(groupPattern, coords.getBuildName(), projectName, coords.getGroup(), coords.getVersion(), platform.getName(), module.getName());
        final String name = properties.resolvePattern(namePattern, coords.getBuildName(), projectName, coords.getGroup(), coords.getVersion(), platform.getName(), module.getName());
        final String version = this.version.toString();

        final List<File> internals = new ArrayList<>();
        final List<File> externals = new ArrayList<>();
        final List<File> projects = new ArrayList<>();
        final List<File> unknown = new ArrayList<>();

        File coord = new File(indexRoot, "coord");
        File groupDir = new File(coord, group);
        File moduleDir = new File(groupDir, name);
        File versionDir = new File(moduleDir, version);
        if (versionDir.isDirectory()) {
            for (File dep : versionDir.listFiles()) {
                if (dep.isDirectory()) {
                    final File[] children = dep.listFiles();
                    if (children != null) {
                        List<File> files = Arrays.asList(children);
                        switch (dep.getName()) {
                            case "internal":
                                // internal dependencies are pointers to other plat:mod dependencies,
                                // and are always created, even for dead dependency chains.
                                internals.addAll(files);
                                break;
                            case "external":
                                // internal dependencies are pointers to other plat:mod dependencies,
                                // and are always created, even for dead dependency chains.
                                externals.addAll(files);
                                break;
                            case "projects":
                                // internal dependencies are pointers to other plat:mod dependencies,
                                // and are always created, even for dead dependency chains.
                                projects.addAll(files);
                                break;
                            case "unknown":
                                // internal dependencies are pointers to other plat:mod dependencies,
                                // and are always created, even for dead dependency chains.
                                unknown.addAll(files);
                                break;
                            default:
                                throw new IllegalStateException("Unexpected directory named " + dep.getAbsolutePath());
                        }
                    }
                } else {
                    unknown.add(dep);
                }
            }
        }

        // now, read in the liveness file...
        // without explicit dependencies, we need to check for a liveness level > 2
        String projectPath = mangleProjectPath(projectName);
        File pathDir = new File(indexDir, "path");
        File projectDir = new File(pathDir, projectPath);
        final String platMod = PlatformModule.unparse(platform.getName(), module.getName());
        final File modDir = new File(projectDir, platMod);
        // hm... we may want to get more picky, like "check for sources file", "check for explicit 'create this' flag", etc.
        File liveFile = new File(modDir, "live");
        String liveValue = "0";
        if (liveFile.exists()) {
            liveValue = readFile(liveFile);
        }

        final IndexResult result = new IndexResult(
                unmodifiableList(internals),
                unmodifiableList(externals),
                unmodifiableList(projects),
                unmodifiableList(unknown),
                Integer.parseInt(liveValue)
        );
        return result;
    }

    private String toKey(final String projectName, final String platform, final String module) {
        if (projectName == null || projectName.isEmpty() || ":".equals(projectName)) {
            return platform + " " + module;
        }
        return projectName + " " + platform + " " + module;
    }

}
