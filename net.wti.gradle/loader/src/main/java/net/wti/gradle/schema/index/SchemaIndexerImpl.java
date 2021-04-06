package net.wti.gradle.schema.index;

import com.github.javaparser.ast.expr.UiContainerExpr;
import net.wti.gradle.api.MinimalProjectView;
import net.wti.gradle.require.api.PlatformModule;
import net.wti.gradle.schema.api.QualifiedModule;
import net.wti.gradle.schema.api.SchemaModule;
import net.wti.gradle.schema.api.SchemaPlatform;
import net.wti.gradle.schema.api.SchemaProject;
import net.wti.gradle.schema.map.SchemaMap;
import net.wti.gradle.schema.api.SchemaDependency;
import net.wti.gradle.schema.parser.DefaultSchemaMetadata;
import net.wti.gradle.schema.parser.SchemaParser;
import net.wti.gradle.schema.spi.SchemaIndex;
import net.wti.gradle.schema.spi.SchemaIndexer;
import net.wti.gradle.schema.spi.SchemaProperties;
import org.gradle.internal.exceptions.DefaultMultiCauseException;
import org.gradle.util.GFileUtils;
import xapi.fu.Lazy;
import xapi.fu.Out1;
import xapi.fu.log.Log;
import xapi.fu.log.Log.LogLevel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static xapi.util.X_String.isNotEmptyTrimmed;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 2020-07-20 @ 2:17 a.m..
 */
public class SchemaIndexerImpl implements SchemaIndexer {

    private final SchemaProperties properties;

    public SchemaIndexerImpl(final SchemaProperties properties) {
        this.properties = properties;
    }

    private class IndexingParser implements SchemaParser {

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
            return platform;
        }

        @Override
        public SchemaModule insertModule(
                SchemaProject project, DefaultSchemaMetadata metadata, SchemaModule module, UiContainerExpr source
        ) {
            project.addModule(module);
            return module;
        }

        @Override
        public SchemaProperties getProperties() {
            return properties;
        }

    }

    @Override
    public Out1<SchemaIndex> index(MinimalProjectView view, String buildName, File rootDir) {
        ExecutorService exec = Executors.newWorkStealingPool(4);
        List<Future<?>> futures = new CopyOnWriteArrayList<>();

        File indexDir = calculateIndex(view, buildName, rootDir);
        final SchemaIndexBuilder index = new SchemaIndexBuilder(view, rootDir, properties, exec, futures, indexDir);

        populateIndex(view, index, buildName);

        final Future<SchemaIndex> waiter = exec.submit(Out1.out1Unsafe(()->{
            // block until index is fully loaded.
            long ttl = getMaxWaitMillis();
            long tooLong = System.currentTimeMillis() + ttl;
            final List<Throwable> failures = new ArrayList<>();
            SchemaIndex result = null;
            for (;result == null || !futures.isEmpty();) {
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
                        } catch (ExecutionException | TimeoutException e) {
                            failures.add(e);
                        }
                    }
                    return false;
                });
                // rethrow any failures
                if (!failures.isEmpty()) {
                    throw new DefaultMultiCauseException("Indexing failures encountered for " + buildName + " @ file://" + rootDir, failures);
                }
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
                if (result == null) {
                    // finalize the index, but only once! after that, we just drain callbacks
                    result = index.build();
                }
            } // end for(;;){}
            return result;

        })::out1);
        // transform future into our Lazy wrapper type.
        return Lazy.deferred1Unsafe(()-> {
            // wait for the main index parse
            final SchemaIndex result = waiter.get();
            // now, run an analyze, so any transitive / interim dependencies
            analyzeIndex(view, result, index);
            return result;
        });
    }

    private File calculateIndex(final MinimalProjectView view, final String buildName, final File rootDir) {

        final String configuredLoc = properties.getIndexLocation(view);
        final File indexDir;
        if (configuredLoc == null) {
            // user hasn't specified, start guessing-and-then-default-to ./build/index
            final String loc = SchemaIndexer.getIndexLocation(view, properties);
            indexDir = new File(loc);
        } else {
            indexDir = new File(configuredLoc);
        }

        if (!indexDir.mkdirs()) {
            // hm... should log that we lack permission to r/w the indexDir... we care, but not enough to break...
        }
        if (!indexDir.isDirectory()) {
            throw new IllegalStateException("Unable to create index directory " + indexDir);
        }
        return indexDir;
    }

    private void analyzeIndex(final MinimalProjectView view, final SchemaIndex result, final SchemaIndexBuilder index) {
        // search through, and discover the full set of "project:platform:module" that we want to consider realized.
        // we first want to mark anything with source or explicitly marked as included
        // next, anything which depends on said modules should be marked as included.

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
                IndexingParser parser = new IndexingParser(view, index);
                SchemaMap map = SchemaMap.fromView(view.findView(":"), parser);

                map.getResolver().out1(); // ensure this lazy is initialized.
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
        SchemaMap map
    ) {
//        final File indexDir = index.getIndexDir();
        for (SchemaProject project : map.getAllProjects()) {
            File projectDir = index.calcProjectDir(project);
            projectDir.mkdirs();
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
                    if (srcDir.isDirectory()) {
                        // yay! record that this module has sources.
                        File moduleDir = new File(projectDir, configName);
                        GFileUtils.writeFile(srcDir.getAbsolutePath(),
                            new File(moduleDir, "sources"), "UTF-8");
                    }

                    // Ok, we now have a coords/group/name/version directory, place our dependency information there.
                    for (SchemaDependency dep : project.getDependenciesOf(platform, module)) {
                        switch (dep.getType()) {
                            case unknown:
                            case project:
                                File projectDeps = new File(versionDir.out1(), "project");
                                if (!projectDeps.isDirectory() && !projectDeps.mkdirs()) {
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
                                if (!internalDeps.isDirectory() && !internalDeps.mkdirs()) {
                                    // we should throw some generic "can't write files" error here...
                                    throw new IllegalStateException("Unable to create projects directory " + internalDeps + "; check disk usage and filesystem permissions");
                                }
                                name = dep.getName();
                                depFile = new File(internalDeps, name);
                                // hm, we have nothing interesting to write into internal dependencies, the filename is a key...
                                GFileUtils.touch(depFile);
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



                } // end all modules
            } // end all platforms
        } // end all projects
    }

    private void recordEdges(final MinimalProjectView view, final SchemaProject project, final SchemaPlatform platform, final SchemaModule module, final SchemaDependency dep, final SchemaIndexBuilder index) {
        // record in/ and out/ edges for each project:platform:module -> p:p:m pairing.
        // The in/ edge for module A is the dependencies of module A
        // The out/ edge for module A is the modules which depend on module A

        File requestorDir = index.calcProjectDir(project, platform.getName(), module.getName());
        File requestedDir = index.calcDependencyProjectDir(view, dep, project, platform, module);
        File inDir = new File(requestorDir, "in");
        File outDir = new File(requestedDir, "out");

        System.out.println(inDir + " <-- " + outDir);
        // Now, create filesystem entries;
        // out metadata goes into the inDir
        // in metadata goes the outDir.

        // in metadata is built from the declaring SchemaProject, SchemaPlatform and SchemaModule,
        String ppmIn = index.calcPPM(project, platform, module);
        // out metadata is built from the declaring SchemaDependency, with defaults based on the requesting project, platform and module.



//        StringBuilder pathRequestor = new StringBuilder()
//            .append(platform.getName())
//            .append(File.separator)
//            .append(module.getName())
//            ;
//        StringBuilder pathRequested = new StringBuilder()
//            .append(dep.getPlatformOrDefault())
//            .append(File.separator)
//            .append(dep.getModuleOrDefault())
//            .append(File.separator)
//            .append(dep.getName())
//            ;
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
        GFileUtils.writeFile(platType + ":" + modType, depFile, "utf-8");
    }

    protected String guessBuildName(File rootDir) {
        // TODO: check w/ git
        return rootDir.getAbsoluteFile().getName();
    }
}
