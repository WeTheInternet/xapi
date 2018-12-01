package xapi.dist.impl;

import xapi.bytecode.ClassFile;
import xapi.dev.api.Dist;
import xapi.dev.api.MavenLoader;
import xapi.dev.impl.ReflectiveMavenLoader;
import xapi.dev.template.CompilationFailed;
import xapi.dist.api.DistOpts;
import xapi.dev.resource.impl.SourceCodeResource;
import xapi.dev.scanner.api.ClasspathScanner;
import xapi.dev.scanner.impl.ClasspathResourceMap;
import xapi.file.X_File;
import xapi.fu.itr.MappedIterable;
import xapi.fu.Out1;
import xapi.fu.Out2;
import xapi.inject.X_Inject;
import xapi.javac.dev.api.CompilerService;
import xapi.javac.dev.model.CompilerSettings;
import xapi.javac.dev.model.CompilerSettings.ImplicitMode;
import xapi.javac.dev.model.CompilerSettings.PreferMode;
import xapi.log.X_Log;
import xapi.reflect.X_Reflect;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main class for generating distribution builds, which includes
 * processing {@link Dist} annotations, performing magic method injection
 * and other source-to-source transformations.
 *
 * Should only be run just before creating
 *
 * Exists primarily to standardize and ease calling into {@link DistPlugin}.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 11/12/17.
 */
public class DistGenerator {

    static ThreadLocal<DistGenerator> GENERATOR = new ThreadLocal<>();
    private ClasspathResourceMap resources;
    private String outputDir;
    private DistOpts opts;
    private String[] distClasses;

    public static void main(String ... args) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler((t, e)->{
            e.printStackTrace();
        });
        new DistGenerator().process(new DistOpts(), args);
    }

    public void process(DistOpts opts, String ... args) throws Exception {
        assert GENERATOR.get() == null : "You _REALLY_ should only be running one DistGenerator at once," +
            " and only calling process() once.";
        GENERATOR.set(this);
        try {

            this.opts = opts;
            final String[] remaining = opts.processArgs(args);

            MappedIterable<String> entryPoints = opts.getEntryPoints();
            // TODO add DistOpts for finer grain scanning
            final ExecutorService pool = Executors.newCachedThreadPool();
            final Callable<ClasspathResourceMap> targets =
                X_Inject.instance(ClasspathScanner.class)
                    .matchClassFile(".*")
                    .matchSourceFile(".*")
//                    .matchResource(".*[.]java")
                    .scanPackage("")
                    .scan(Thread.currentThread().getContextClassLoader(),
                        pool);
            final Set<String> cp = new LinkedHashSet<>();
            MavenLoader loader = new ReflectiveMavenLoader();
            final Out1<Iterable<String>> javac = loader.downloadDependency(loader.getDependency("xapi-dev-javac"));
            // TODO refactor dist generator into its own module, and load that instead...
            final Out1<Iterable<String>> dev = loader.downloadDependency(loader.getDependency("xapi-dev"));

            final String loc = X_Reflect.getFileLoc(DistPlugin.class);
            resources = targets.call();
            if (entryPoints.isEmpty()) {
                entryPoints = resources.findClassAnnotatedWith(Dist.class)
                    .map(ClassFile::getQualifiedName)
                    .cached();
            }
            distClasses = entryPoints.toArray(String[]::new);
            if (entryPoints.isEmpty()) {
                X_Log.info(DistGenerator.class, "No @Dist files found to process");
            }
            javac.out1().forEach(cp::add);
            dev.out1().forEach(cp::add);

            CompilerService service = X_Inject.singleton(CompilerService.class);
            outputDir = X_File.createTempDir("xapiDist").getAbsolutePath();
            final CompilerSettings settings = service.defaultSettings()
                .setImplicitMode(ImplicitMode.CLASS)
                .setPlugins(Collections.singletonList(DistPlugin.class.getSimpleName()))
                .setPreferMode(PreferMode.SOURCE)
                .setVerbose(true)
                .setUseRuntimeClasspath(true)
                .setOutputDirectory(outputDir)
                .setProcessorPath(
                    MappedIterable.mapped(cp).join(File.pathSeparator)
                )
                .setClasspath(cp)
                ;
            final String[] entryClasses = resources.findSources("")
                .map(SourceCodeResource::getResourceName)
                .toArray(String[]::new);
            final Out2<Integer, URL> result = service.compileFiles(
                settings,
                entryClasses
            );
            int code = result.out1();
            if (code == 0) {
                // huzzah!  Lets sanity test our newly compiled files.
                final URL[] urls = {result.out2()};
                URLClassLoader cl = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
                for (String e : entryClasses) {
                    try {
                        cl.loadClass("xapi" + e.split("java/xapi")[1].replace('/', '.').replace(".java", ""));
                    } catch (ClassNotFoundException ex) {
                        X_Log.error(DistGenerator.class, "Unable to load class", e, ex);
                    }
                }
            } else {
                X_Log.error(DistGenerator.class, "javac returned ", code, " while running on ", entryPoints, "Check previous log for errors");
                throw new CompilationFailed("javac returned " + code + " for " + entryPoints.join(","));
            }
        } finally {
            GENERATOR.remove();
        }
    }

    public ClasspathResourceMap getResources() {
        return resources;
    }
}
