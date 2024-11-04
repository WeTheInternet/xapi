package net.wti.gradle.settings.index;

import net.wti.gradle.api.MinimalProjectView;
import net.wti.gradle.settings.XapiSchemaParser;
import net.wti.gradle.settings.api.*;
import net.wti.gradle.settings.schema.DefaultSchemaMetadata;
import net.wti.gradle.tools.GradleFiles;
import net.wti.gradle.tools.HasAllProjects;
import net.wti.javaparser.ast.expr.UiContainerExpr;
import org.gradle.internal.exceptions.DefaultMultiCauseException;
import xapi.fu.*;
import xapi.fu.log.Log;
import xapi.fu.log.Log.LogLevel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static net.wti.gradle.tools.GradleFiles.*;
import static xapi.string.X_String.isNotEmptyTrimmed;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 2020-07-20 @ 2:17 a.m..
 */
public class SchemaIndexerImpl implements SchemaIndexer {

    public static final String EXT_NAME = "_xapiBuildIndexer";

    private final SchemaProperties properties;
    private final IndexNodePool nodes;

    public SchemaIndexerImpl(final SchemaProperties properties, final IndexNodePool nodePool) {
        this.properties = properties;
        this.nodes = nodePool;
    }

    private class IndexingParser implements XapiSchemaParser {

        private final MinimalProjectView view;
        private final SchemaIndexBuilder index;

        public IndexingParser(
                MinimalProjectView view,
                SchemaIndexBuilder index) {
            this.view = view;
            this.index = index;
        }

        @Override
        public MinimalProjectView getView() {
            return view;
        }

        @Override
        public SchemaPlatform insertPlatform(
                SchemaProject project, DefaultSchemaMetadata metadata, SchemaPlatform platform, UiContainerExpr source
        ) {
            project.addPlatform(platform);
            // TODO: prepare recording of platform in index
            return platform;
        }

        @Override
        public SchemaModule insertModule(
                SchemaProject project, DefaultSchemaMetadata metadata, SchemaModule module, UiContainerExpr source
        ) {
            project.addModule(module);
            // TODO: prepare recording of module in index
            return module;
        }

        @Override
        public SchemaProperties getProperties() {
            return properties;
        }

    }

    @Override
    public Out1<SchemaIndex> index(MinimalProjectView view, String buildName, HasAllProjects map) {
        String indexProp = properties.getIndexIdProp(view);
        boolean alreadyDone = "true".equals(properties.getProperty(view, indexProp));

        ExecutorService exec = Executors.newWorkStealingPool(4);
        List<Future<?>> futures = new CopyOnWriteArrayList<>();
        final File rootDir = view.getProjectDir();

        File indexDir = calculateIndexDir(view, buildName, rootDir);
        final SchemaIndexBuilder index = new SchemaIndexBuilder(view, nodes, rootDir, properties, exec, futures, indexDir);

        if ("true".equals(System.getProperty(indexProp))) {
            return Out1.immutable(index);
        }
        try {
            System.setProperty(indexProp, "running");
        } catch (Exception e) {
            Log.loggerFor(view).log(SchemaIndexerImpl.class, LogLevel.ERROR,
                    "Unable to set indexProp ", indexProp, " to running state", e);
        }
        populateIndex(view, map, index, buildName);

        final List<Throwable> failures = new ArrayList<>();
        final String errMsg = "Indexing failures encountered for " + buildName + " @ file://" + rootDir;
        final Out1<RuntimeException> throwFailures = ()->{
            if (!failures.isEmpty()) {
                properties.markFailed(indexProp, view, "index job");
                if (failures.size() == 1) {
                    Throwable only = failures.get(0);
                    if (only instanceof ExecutionException && only.getCause() != null) {
                        only = only.getCause();
                    }
                    if (only instanceof IndexingFailedException) {
                        throw new IndexingFailedException(errMsg + " : " + only.getMessage());
                    }
                    throw new IndexingFailedException(errMsg, only);
                }
                throw new DefaultMultiCauseException(errMsg, failures);
            }
            return null;
        };
        final Future<SchemaIndex> waiter = exec.submit(Out1.out1Unsafe(()->{
            // block until index is fully loaded.
            long ttl = getMaxWaitMillis();
            long tooLong = System.currentTimeMillis() + ttl;
            SchemaIndex result = null;
            for (;!futures.isEmpty();) {
                // clean out stale / complete futures
                futures.removeIf(future -> {
                    if (future.isDone()) {
                        try {
                            future.get(1, TimeUnit.MILLISECONDS);
                            return true;
                        } catch (InterruptedException e) {
                            // capture any failures
                            Thread.currentThread().interrupt();
                            failures.add(e);
                        } catch (ExecutionException e) {
                            final Throwable cause = e.getCause() == null ? e : e.getCause();
                            failures.add(cause);
                        } catch (TimeoutException e) {
                            // really shouldn't timeout if future.isDone() is true, above
                            failures.add(e);
                        }
                    }
                    return false;
                });
                // rethrow any failures

                throwFailures.out1();

                // check if we're done
                synchronized (futures) {
                    // if no futures were added, we never block, below.
                    if (futures.isEmpty()) {
                        break;
                    }
                    futures.wait(500);
                }
                // check if we've taken too long
                if (System.currentTimeMillis() > tooLong) {
                    throw new TimeoutException("Took " + ttl + " millis to build index for " + rootDir + " (" + buildName + ")");
                }
            } // end for(;;){}
            // now, run an analyze, so we can mark all live transitive / interim dependencies
            analyzeIndex(view, index, map, properties, nodes);
            // TODO: mark/sweep old modules somehow... in case some poor soul tries to read all "/live" files or something silly
            // finalize our index results
            result = index.build();
            return result;

        })::out1);
        // transform future into our Lazy wrapper type.
        // This object exists as the "place to go to wait until all schema files are read"... we should not analyze it right away though.
        // We need to submit an interim object, so we can mess with the post-parse results before we analyze the index.
        // Dependency-resolution-plans
        return Lazy.deferred1Unsafe(()-> {
            // wait for the main index parse
            final SchemaIndex result;
            try {
                result = waiter.get();
            } catch (Exception e) {
                // add this failure to exceptions; this ensures we don't NPE when we throw the results of calling throwFailures.
                Throwable f;
                if (e instanceof ExecutionException && e.getCause() != null) {
                    f = e.getCause();
                } else {
                    f = e;
                }
                if (!failures.contains(f)) {
                    failures.add(0, f);
                }
                throw throwFailures.out1();
            }
            // set a system property to say "this index is done".
            // hmm... Lazy framework can get dirty, if somebody composes our caller in Lazy code, our markDone() might be premature.
            properties.markDone(indexProp, view, "index job");
            // TODO: composable Lazy where .whenDone() waits until root-most lazy is finished...
            //   this way, if we need to do something thread-sensitive, like acquire a lock or launch off-threadable tasks,
            //   and wait until "all Lazy in the current stack are done" to call said callbacks.
            return result;
        });
    }

    private File calculateIndexDir(final MinimalProjectView view, final String buildName, final File rootDir) {

        final String configuredLoc = SchemaProperties.getIndexLocation(view, properties);
        final File indexDir = new File(configuredLoc);

        if (!indexDir.mkdirs()) {
            // hm... should log that we lack permission to r/w the indexDir... we care, but not enough to break...
        }
        if (!indexDir.isDirectory()) {
            throw new IllegalStateException("Unable to create index directory " + indexDir);
        }
        return indexDir;
    }

    private void analyzeIndex(final MinimalProjectView view, final SchemaIndexBuilder index, final HasAllProjects map, final SchemaProperties properties, final IndexNodePool nodes) {
        // search through, and discover the full set of "project:platform:module" that we want to consider realized.
        // we first want to mark anything with source or explicitly marked as included
        // next, anything which depends on said modules should be marked as included.

        // to mark something as explicit, we should accept: anything adding via a require=... attribute,
        // or anything w/ a schema property matching the ppm-name of the module (_mangled_project_path:platform:module)

        // from there, transverse all "in" edges, marking such modules as required,
        // also consider marking all "out" edges as "has live input", so said modules can quickly realize they have valid dependencies.

        for (SchemaProject project : map.getAllProjects()) {
            final MinimalProjectView pv = project.getView();
            project.forAllPlatformsAndModules((plat, mod)-> {
                // hokay! check if the module has sources, or is explicitly marked as live via properties.
                File projDir = pv.getProjectDir();
                File projSrc = new File(projDir, "src");
                PlatformModule platMod = nodes.getPlatformModule(plat.getName(), mod.getName());
                final ModuleIdentity id = nodes.getIdentity(pv, project.getPathGradle(), platMod);
                final IndexNode node = nodes.getNode(id);
                String modulePrefix = QualifiedModule.unparse(plat.getName(), mod.getName());
                File moduleSrc = new File(projSrc, modulePrefix);
                if (hasSources(moduleSrc)) {
                    // This module is live due to having sources!
                    index.markWithSource(project, plat, mod, moduleSrc);
                    node.addLiveness(LivenessReason.has_source);
                } else {
                    String mangleProject = QualifiedModule.mangleProjectPath(project.getPathGradle());
                    File modDir = index.calcProjectDir(project, plat.getName(), mod.getName());
                    File force = new File(modDir, "force");
                    if (force.exists()) {
                        final String forceContents = readFile(force);
                        if (!"false".equals(forceContents)) {
                            index.markExplicitInclude(project, plat, mod);
                            node.addLiveness(LivenessReason.forced);
                        }
                    } else if (project.hasExplicitDependencies(platMod)){
                        index.markExplicitInclude(project, plat, mod);
                        node.addLiveness(LivenessReason.has_dependencies);
                    } else {
                        // check if this module should be live b/c of schema properties
                        String liveKey = "live_" + mangleProject + "_" + modulePrefix;
                        final String liveValue = properties.getProperty(view, liveKey);
                        if ("true".equals(liveValue)) {
                            index.markExplicitInclude(project, plat, mod);
                            node.addLiveness(LivenessReason.forced);
                        }
                    }
                }
                // now... anybody who depends on us also needs to be a live module...
            });
        }
        // flush callbacks so we mark all live modules.
        map.flushWork();

        // now, we should be able to prune and compress the IndexNode graph
        index.compressNodes(nodes);

        // now, transverse all explicitly-live modules, and mark anything depending on these modules as also-live.
        index.forAllLiveModules(liveness -> {
            // increase the liveness level to 2 for each "this module is actually live",
            // and liveness level 1 for "this module depends on something that is live".
            File projectDir = liveness.getProjectDir();
            File liveFile = new File(projectDir, "live");
            writeFile(liveFile, liveness.isForce() ? "3" : liveness.isExplicit() ? "4" : "2");
            // now, go through all of this project's out/ edges, marking each one as at-least-level-1
            markOutEdges(map, index, liveness.getProjectDir());
//            markInEdges(map, index, liveness.getProjectDir());
            // now, also write some other useful metadata
            File multiplatformFile = new File(projectDir, "multiplatform");
            File virtualFile = new File(projectDir, "virtual");
            File forceFile = new File(projectDir, "force");
            writeFile(multiplatformFile, Boolean.toString(liveness.isMultiplatform()));
            writeFile(virtualFile, Boolean.toString(liveness.isVirtual()));
            if (liveness.isForce()) {
                writeFile(forceFile, "true");
            } else if (forceFile.exists()) {
                forceFile.delete();
            }
        });

        // next, inspect all index results with more than one live result, to mark as multiplatform.
        final File[] allProjs = index.getDirByPpm().listFiles();
        if (allProjs == null) {
            throw new IllegalStateException("Unable to read directory file://" + index.getDirByPpm());
        }
        for (File projectDir : allProjs) {
            int modCount = 0;
            if (projectDir.isDirectory()) {
                final File[] allPlatMods = projectDir.listFiles();
                if (allPlatMods == null) {
                    throw new IllegalStateException("Unable to read directory file://" + projectDir);
                }
                for (File platModDir : allPlatMods) {
                    if (platModDir.isDirectory()) {
                        File liveFile = new File(platModDir, "live");
                        String contents = readFile(liveFile);
                        if (contents.isEmpty() || "0".equals(contents)) {
                            // not live, do not count
                        } else {
                            modCount ++;
                        }
                    }
                }
            }
            if (modCount > 1) {
                // forcibly write a multiplatform file. TODO: validate it wasn't explicitly marked single-platform
                File multiplatformFile = new File(projectDir, "multiplatform");
                writeFile(multiplatformFile, "true");
            }
        }
    }

    private boolean hasSources(final File moduleSrc) {
        File gradleSrc = new File(moduleSrc.getParent(), "gradle/" + moduleSrc.getName());
        if (gradleSrc.isDirectory()) {
            final File[] children = gradleSrc.listFiles();
            if (children != null && children.length > 0) {
                return true;
            }
        }
        if (!moduleSrc.isDirectory()) {
            return false;
        }
        final File[] children = moduleSrc.listFiles();
        if (children == null) {
            return false;
        }
        for (File file : children) {
            if (file.isDirectory() && ! "build".equals(file.getName())) {
                return true;
            }
        }
        return false;
    }

    private void markOutEdges(final HasAllProjects map, final SchemaIndexBuilder index, final File projectDir) {
        File outDir = new File(projectDir, "out");
        if (outDir.isDirectory()) {
            for (File mangledProjects : outDir.listFiles()) {
                for (File platMods : mangledProjects.listFiles()) {
                    File linkFile = new File(platMods, "link");
                    String nextPath = readFile(linkFile);
                    final File readLink = new File(index.getDirByPpm(), nextPath);
                    File liveFile = new File(readLink, "live");
                    String value = readFile(liveFile);
                    int intVal = Integer.parseInt(value);
                    if (intVal < 1) {
                        // only create a "1" file if a child edge results in a >1 liveness.
                        writeFile(liveFile, "1");
                        markOutEdges(map, index, readLink);
                    }
                }

            }

        }
    }

    private long getMaxWaitMillis() {
        // a minute to parse and write index is tooooo much.
        // if we really take this long, there's got to be bugs to fix...
        String env_limit;
        env_limit = System.getProperty("xapi.schema.index.ttl");
        if (env_limit == null) {
            env_limit = System.getenv("XAPI_SCHEMA_INDEX_TTL");
        }
        if (env_limit != null) {
            return Long.parseLong(env_limit);
        }
        return 60_000;
    }

    protected void populateIndex(
            MinimalProjectView view,
            final HasAllProjects map,
            SchemaIndexBuilder index,
            String buildName
    ) {
        if (buildName == null) {
            buildName = view.getBuildName();//guessBuildName(rootDir);
        }
        index.setBuildName(buildName);

        ExecutorService exec = index.getExecutor();
        List<Future<?>> futures = index.getFutures();
        File rootDir = index.getRootDir();

        File schemaFile = new File(rootDir, "schema.xapi"); // TODO schema-lock.xapi?
        // currently, no need to parse/build anything if there's no root schema.xapi.
        if (schemaFile.isFile()) {
            // an array of size one is just a simple pointer (that we can also use as a mutex).
            final Future<?>[] future = new Future[1];
//            SchemaIndexBuilder localIndex = index.duplicate();
            future[0] = exec.submit(()->{
                map.resolve();
                if (index.getGroupId() == null || QualifiedModule.UNKNOWN_VALUE.equals(index.getGroupId().toString())) {
                    index.setGroupId(map.getGroup());
                }
                if (index.getVersion() == null || QualifiedModule.UNKNOWN_VALUE.equals(index.getVersion().toString())) {
                    index.setVersion(map.getVersion());
                }
                // persist everything to disk... really, we should just supply a parser that submits-as-it-goes
                writeIndex(view, index, map);

                // let anyone blocking on us finish.
                synchronized (futures) {
                    // we're using a concurrent list, but we need to take the lock to notifyAll() anyway, so might as well synchronize first.
                    futures.remove(future[0]);
                    future[0]=null;
                    futures.notifyAll();
                }
            });
            futures.add(future[0]);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    protected void writeIndex(
            MinimalProjectView view,
            SchemaIndexBuilder index,
            HasAllProjects map
    ) {
//        final File indexDir = index.getIndexDir();
        for (SchemaProject project : map.getAllProjects()) {
            File projectDir = index.calcProjectDir(project);
            projectDir.mkdirs();
            // hm... we should use index.getReader(), so we can pre-prime our reader (save a set of disk reads later)
            if (!projectDir.isDirectory()) {
                throw new IllegalStateException("Unable to create index project directory " + projectDir);
            }
            for (SchemaPlatform platform : project.getAllPlatforms()) {
                String groupResolved = properties.resolvePattern(platform.getPublishPattern(), index, project.getPublishedName(), platform.getName(), "");

                // modules should probably come from the platform, not the project...
                for (SchemaModule module : project.getAllModules()) {
                    if (!platform.isPublished() || !module.isPublished()) {
                        // TODO: write out a non-published, "private module" file, so we can accurately say "your coordinates exist, but the target is private"
                        //   instead of "coordinates not found".
                        continue;
                    }
                    // setup a publishing target.
                    String configName = PlatformModule.unparse(platform.getName(), module.getName());

                    // Let our SchemaProperties decide how to resolve names (so end user can override at will)
                    String nameResolved = properties.resolvePattern(module.getPublishPattern(), index, project.getPublishedName(), platform.getName(), module.getName());

                    Log.firstLog(this, map, index, view) // none of these objects, by default implement Log, but you can intercept them to add Log interface
                            .log(SchemaIndexerImpl.class, LogLevel.TRACE, Out1.newOut1( ()-> // make the string below lazily-computer
                                    groupResolved+":"+nameResolved+":" + map.getVersion() +
                                            " -> " +
                                            project.getPublishedName() +":" + platform.getName()+":"+module.getName()+"?" + platform.isPublished() + ":" + module.isPublished()
                            ));
                    String version = map.getVersion();
                    Lazy<File> versionDir = index.getDirGNV(groupResolved, nameResolved, version);


                    // Check for sources, so we can record that this module is "live"
                    File srcDir = new File(project.getView().getProjectDir(), "src/" + configName);
                    boolean hasSrc = hasSources(srcDir);
                    if (hasSrc) {
                        // yay! record that this module has sources.
                        File moduleDir = new File(projectDir, configName);
                        writeFile(new File(moduleDir, "sources"), srcDir.getAbsolutePath());
                    }

                    // Ok, we now have a coords/group/name/version directory, place our dependency information there.
                    for (SchemaDependency dep : project.getDependenciesOf(platform, module)) {
                        switch (dep.getType()) {
                            case unknown:
                            case project:
                                File projectDeps = new File(versionDir.out1(), "project");
                                if (!projectDeps.isDirectory() && !projectDeps.mkdirs() && !projectDeps.isDirectory()) {
                                    // we should throw some generic "can't write files" error here...
                                    throw new IllegalStateException("Unable to create project directory " + projectDeps + "; check disk usage and filesystem permissions");
                                }
                                String name = dep.getName();
                                File depFile = new File(projectDeps, name);
                                // write platform:module coordinate into dependency file (incoming edge)
                                extractCoords(platform, module, dep, depFile);
                                // record incoming/outgoing edges
                                recordEdges(view, project, platform, module, dep, index);
                                break;
                            case internal:
                                File internalDeps = new File(versionDir.out1(), "internal");
                                if (!internalDeps.isDirectory() && !internalDeps.mkdirs() && !internalDeps.isDirectory()) {
                                    // we should throw some generic "can't write files" error here...
                                    throw new IllegalStateException("Unable to create projects directory " + internalDeps + "; check disk usage and filesystem permissions");
                                }
                                name = dep.getName();
                                String mangledName = PlatformModule.parse(name).toPlatMod();
                                depFile = new File(internalDeps, mangledName);
                                // hm, we have nothing interesting to write into internal dependencies, the filename is a key...
                                touch(depFile);
                                if (!depFile.isFile()) {
                                    throw new IllegalStateException(depFile + " does not exist after touching it; check filesystem usage (df -h) or existence+permissions (ls -la)");
                                }
                                recordEdges(view, project, platform, module, dep, index);
                                break;
                            case external:
                                String depGroup = dep.getGroup();
                                String depVersion = dep.getVersion();
                                name = dep.getName();
                                final String extraGnv = dep.getExtraGnv();
                                // hm... this is where we should probably record something that can wait to try to resolve
                                // an external dependency w/ metadata from previously-indexed sub-builds.
                                // Only if there is no sub-build which, by schema.xapi, claims a G:N[:V],
                                // will we simply write out the full "g:n:v[:extras]" structure;
                                // otherwise, we should convert this dependency to a "foreign" layer
                                depFile = new File(versionDir.out1(), depGroup + ":" + name +  ":" + depVersion +
                                        (isNotEmptyTrimmed(extraGnv) ? ":" + extraGnv : ""));
                                extractCoords(platform, module, dep, depFile);
                                break;
                        }

                    } // end all getDependenciesOf(platform, module)

                    // all modules should record a little state about themselves into the index,
                    // in such a way that a single file read can reveal a single piece of information about this module.
                    // This allows interested parties to register arbitrary index files as task inputs,
                    // to be able to invalidate a task's cache if "some interesting state" changes in the index.
                    // While not recommended for "customers" to implement this kind of stuff,
                    // we'll make all kinds of internal cool stuff to be able to expose this in a more stable way:
                    // SentinelTask, which act as a "smart lifecycle task" for arbitrary user-supplied tasks.
                    // They can hook up index files as task inputs, and serve as cheaply-realizable task graph nodes
                    // which you can pre-create and then call inside a tasks.register() callback (where it's illegal to create new tasks).
                    File modDir = new File(projectDir, configName);

                    final File testFile = new File(modDir, "test");
                    final File publishFile = new File(modDir, "publish");
                    final File forceFile = new File(modDir, "force");
                    final File liveFile = new File(modDir, "live");
                    if (module.isTest()) {
                        writeFile( testFile, "true");
                    } else if (testFile.exists()){
                        testFile.delete();
                    }
                    if (module.isPublished()) {
                        writeFile(publishFile, "true");
                    } else if (publishFile.exists()) {
                        publishFile.delete();
                        GradleFiles.deleteQuietly(publishFile);
                    }
                    if (module.isForce()) {
                        writeFile(forceFile, "true");
                        writeFile(liveFile, "3");
                    } else if (forceFile.exists()) {
                        forceFile.delete();
                    }
                    if (!liveFile.exists()) {
                        writeFile(liveFile, "0");
                    }
                } // end all modules
            } // end all platforms
        } // end all projects
    }

    private void recordEdges(final MinimalProjectView view, final SchemaProject project, final SchemaPlatform platform, final SchemaModule module, final SchemaDependency dep, final SchemaIndexBuilder index) {
        // record in/ and out/ edges for each project:platform:module -> p:p:m pairing.
        // The in/ edge for module A is the dependencies of module A
        // The out/ edge for module A is the modules which depend on module A

        File requestorDir = index.calcProjectDir(project, platform.getName(), module.getName());
        File requestedDir = index.calcDependencyProjectDir(dep, project, platform, module);
        File inDir = new File(requestorDir, "in");
        File outDir = new File(requestedDir, "out");

        // Now, create filesystem entries;
        // out metadata goes into the inDir
        // in metadata goes the outDir.

        // in metadata is always built from the declaring SchemaProject, SchemaPlatform and SchemaModule,
        // thus, we always know these are going to be project-based addresses
        File ppmOut = index.calcPPM(outDir, project, platform, module);
        // out metadata is built from the declaring SchemaDependency, with defaults based on the requesting project, platform and module.
        // these will be separated between project types, and external dependency types (later: foreign types)
        File ppmIn = index.calcPPM(inDir, dep, project.getPathIndex(), platform.getName(), module.getName());

        // ok, for now, lets just create full directory structures,
        // so we can touch/write to specific files within those directories,
        // to denote whether the given node is live or dead (using symlinks, preferably, to avoid n^2 update costs)
        ppmOut.mkdirs();
        ppmIn.mkdirs();

        File livenessIn = new File(requestorDir, "live");
        File livenessOut = new File(requestedDir, "live");

        // touch / initialize both liveness files, so we'll know if this is an active node or not.
        if (livenessIn.isFile()) {
            touch(livenessIn);
        } else {
            writeFile(livenessIn, "0");
        }
        if (livenessOut.isFile()) {
            touch(livenessOut);
        } else {
            writeFile(livenessOut, "0");
        }

        // Now, write links from ppm directories to matching liveness file
        File inLink = new File(ppmIn, "link");
        File outLink = new File(ppmOut, "link");
        String linkSeg = requestedDir.getAbsolutePath().replace(index.getDirByPpm().getAbsolutePath(), "").substring(1);
        if (inLink.isFile()) {
            String bad;
            assert linkSeg.equals((bad = readFile(inLink))) : "Error, path disagreement in " + inLink + "; was " + bad + "; is " + linkSeg;
        } else {
            writeFile(inLink, linkSeg);
        }

        linkSeg = requestorDir.getAbsolutePath().replace(index.getDirByPpm().getAbsolutePath(), "").substring(1);
        if (outLink.isFile()) {
            String bad;
            assert linkSeg.equals((bad = readFile(outLink))) : "Error, path disagreement in " + outLink + "; was " + bad + "; is " + linkSeg;
        } else {
            writeFile(outLink, linkSeg);
        }

        // hm... submit a task that will check if this dependency is explicit or if the module has sources, and increase liveness count.
    }

    private void extractCoords(SchemaPlatform platform, SchemaModule module, SchemaDependency dep, File depFile) {
        String platType = dep.getCoords().getPlatform();
        if (platType == null) {
            platType = platform.getName();
        }
        String modType = dep.getCoords().getModule();
        if (modType == null) {
            modType = module.getName();
        }
        writeFile(depFile, platType + ":" + modType);
    }

    protected String guessBuildName(File rootDir) {
        // TODO: check w/ git
        return rootDir.getAbsoluteFile().getName();
    }

    public IndexNodePool getNodes() {
        return nodes;
    }
}

