package net.wti.gradle.schema.index;

import com.github.javaparser.ast.expr.UiContainerExpr;
import net.wti.gradle.api.MinimalProjectView;
import net.wti.gradle.schema.api.QualifiedModule;
import net.wti.gradle.schema.api.SchemaModule;
import net.wti.gradle.schema.api.SchemaPlatform;
import net.wti.gradle.schema.api.SchemaProject;
import net.wti.gradle.schema.map.SchemaMap;
import net.wti.gradle.schema.map.internal.SchemaDependency;
import net.wti.gradle.schema.parser.SchemaMetadata;
import net.wti.gradle.schema.parser.SchemaParser;
import net.wti.gradle.schema.spi.SchemaIndex;
import net.wti.gradle.schema.spi.SchemaIndexer;
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

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 2020-07-20 @ 2:17 a.m..
 */
public class SchemaIndexerImpl implements SchemaIndexer {

    private class IndexingParser implements SchemaParser {

        private final MinimalProjectView view;
        private final SchemaIndexBuilder index;
        private final ExecutorService exec;
        private final List<Future<?>> futures;

        public IndexingParser(
                MinimalProjectView view,
                SchemaIndexBuilder index,
                ExecutorService exec,
                List<Future<?>> futures) {
            this.view = view;
            this.index = index;
            this.exec = exec;
            this.futures = futures;
        }

        @Override
        public MinimalProjectView getView() {
            return view;
        }

        @Override
        public void insertPlatform(
            SchemaProject project, SchemaMetadata metadata, SchemaPlatform platform, UiContainerExpr source
        ) {
            project.addPlatform(platform);
        }

        @Override
        public void insertModule(
            SchemaProject project, SchemaMetadata metadata, SchemaModule module, UiContainerExpr source
        ) {
            project.addModule(module);
        }

    }

    @Override
    public Out1<SchemaIndex> index(MinimalProjectView view, String buildName, File rootDir) {
        final SchemaIndexBuilder index = new SchemaIndexBuilder(view, rootDir);
        ExecutorService exec = Executors.newWorkStealingPool(4);
        List<Future<?>> futures = new CopyOnWriteArrayList<>();

        populateIndex(view, index, buildName, rootDir, exec, futures);

        final Future<SchemaIndex> waiter = exec.submit(Out1.out1Unsafe(()->{
            // block until index is fully loaded.
            long ttl = getMaxWaitMillis();
            long tooLong = System.currentTimeMillis() + ttl;
            for (;;) {
                List<Throwable> failures = new ArrayList<>();
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
                    throw new DefaultMultiCauseException("Indexing failures encountered for " + buildName +" @ " + rootDir, failures);
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
            } // end for(;;){}
            // finalize the index.
            return index.build();

        })::out1);
        // transform future into our Lazy wrapper type.
        return Lazy.deferred1Unsafe(waiter::get);
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
        String buildName,
        File rootDir,
        ExecutorService exec,
        List<Future<?>> futures
    ) {
        if (buildName == null) {
            buildName = guessBuildName(rootDir);
        }
        index.setBuildName(buildName);

        File schemaFile = new File(rootDir, "schema.xapi"); // TODO schema-lock.xapi?
        // currently, no need to parse/build anything if there's no root schema.xapi.
        if (schemaFile.isFile()) {
            final Future<?>[] future = new Future[1];
//            SchemaIndexBuilder localIndex = index.duplicate();
            future[0] = exec.submit(()->{
                IndexingParser parser = new IndexingParser(view, index, exec, futures);
                SchemaMap map = SchemaMap.fromView(view.findView(":"), parser);

                map.getResolver().out1(); // ensure this lazy is initialized.
                if (index.getGroupId() == null || QualifiedModule.UNKNOWN_VALUE.equals(index.getGroupId().toString())) {
                    index.setGroupId(map.getGroup());
                }
                if (index.getVersion() == null || QualifiedModule.UNKNOWN_VALUE.equals(index.getVersion().toString())) {
                    index.setVersion(map.getVersion());
                }
                // persist everything to disk... really, we should just supply a parser that submits-as-it-goes
                writeIndex(view, index, map, rootDir, exec, futures);

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
        SchemaMap map,
        File rootDir,
        ExecutorService exec,
        List<Future<?>> futures
    ) {
        final File indexDir = new File(rootDir, "index");
        indexDir.mkdirs();
        if (!indexDir.isDirectory()) {
            throw new IllegalStateException("Unable to create index directory " + indexDir);
        }
        final File byCoord = new File(indexDir, "coord"); // organized by maven publishing coordinates (g:n:v)
        final File byPath = new File(indexDir, "path"); // organized by gradle path $buildName:$project:$path
        // Note that
        for (SchemaProject project : map.getAllProjects()) {
            final String path = project.getPath();
            File projectDir = new File(byPath, path.isEmpty() ? "_" : path);
            projectDir.mkdirs();
            if (!projectDir.isDirectory()) {
                throw new IllegalStateException("Unable to create index project directory " + projectDir);
            }
            for (SchemaPlatform platform : project.getAllPlatforms()) {
                String groupResolved = platform.getPublishPattern()
                        .replaceAll("[$]build", index.getBuildName())
                        .replaceAll("[$]name", project.getPublishedName())
                        .replaceAll("[$]group", index.getGroupIdNotNull())
                        .replaceAll("[$]version", index.getVersion().toString())
                        .replaceAll("[$]platform", platform.getName());

                // modules should probably come from the platform, not the project...
                for (SchemaModule module : project.getAllModules()) {
                    if (platform.isPublished() && module.isPublished()) {
                        // setup a publishing target.

                        // ...need to teach SchemaModule about computing group:name coordinates.
                        String nameResolved = module.getPublishPattern()
                            .replaceAll("[$]build", index.getBuildName())
                            .replaceAll("[$]name", project.getPublishedName())
                            .replaceAll("[$]module", module.getName())
                            .replaceAll("[$]version", index.getVersion().toString())
                            .replaceAll("[$]platform", platform.getName())
                            ;

                        Log.firstLog(this, map, index, view) // none of these objects, by default implement Log, but you can intercept them to add Log interface
                            .log(SchemaIndexerImpl.class, LogLevel.INFO, Out1.newOut1( ()-> // make the string below lazily-computer
                                groupResolved+":"+nameResolved+":" + map.getVersion() +
                                 " -> " +
                                project.getPublishedName() +":" + platform.getName()+":"+module.getName()+"?" + platform.isPublished() + ":" + module.isPublished()
                        ));

                        File groupDir = new File(byCoord, groupResolved);
                        File nameDir = new File(groupDir, nameResolved);
                        File versionDir = new File(nameDir, map.getVersion());
                        versionDir.mkdirs();
                        if (!versionDir.isDirectory()) {
                            throw new IllegalStateException("Unable to create index publishing directory " + versionDir);
                        }

                        // Ok, we now have a coords/group/name/version directory, place our dependency information there.
                        for (SchemaDependency dep : project.getDependenciesOf(platform, module)) {
                            switch (dep.getType()) {
                                case unknown:
                                case project:
                                    File projectDeps = new File(versionDir, "project");
                                    if (!projectDeps.isDirectory() && !projectDeps.mkdirs()) {
                                        // we should throw some generic "can't write files" error here...
                                        throw new IllegalStateException("Unable to create project directory " + projectDeps + "; check disk usage and filesystem permissions");
                                    }
                                    String name = dep.getName();
                                    File depFile = new File(projectDeps, name);
                                    // hm, we actually have nothing interesting to write into the file atm...
                                    // we should, actually, have platform:module coordinate
                                    extractCoords(platform, module, dep, depFile);
                                    break;
                                case internal:
                                    File internalDeps = new File(versionDir, "internal");
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
                                    break;
                                case external:
                                    String depGroup = dep.getGroup();
                                    String depVersion = dep.getVersion();
                                    name = dep.getName();
                                    depFile = new File(versionDir, depGroup + ":" + name +  ":" + depVersion);
                                    extractCoords(platform, module, dep, depFile);
                                    break;
                            }

                        }


                    } // end if (published)

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
