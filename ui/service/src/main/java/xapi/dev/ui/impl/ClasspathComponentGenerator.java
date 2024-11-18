package xapi.dev.ui.impl;

import com.github.javaparser.ASTHelper;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.collect.simple.SimpleStack;
import xapi.dev.X_Dev;
import xapi.dev.lang.gen.ApiGeneratorContext;
import xapi.dev.gen.FileBasedSourceHelper;
import xapi.dev.resource.impl.StringDataResource;
import xapi.dev.scanner.api.ClasspathScanner;
import xapi.dev.scanner.impl.ClasspathResourceMap;
import xapi.dev.scanner.impl.ClasspathScannerDefault;
import xapi.dev.source.DomBuffer;
import xapi.dev.ui.api.*;
import xapi.file.X_File;
import xapi.fu.In1Out1;
import xapi.fu.In2;
import xapi.fu.Lazy;
import xapi.fu.X_Fu;
import xapi.fu.itr.Chain;
import xapi.fu.itr.ChainBuilder;
import xapi.fu.itr.MappedIterable;
import xapi.inject.X_Inject;
import xapi.io.X_IO;
import xapi.lang.oracle.AstOracle;
import xapi.log.X_Log;
import xapi.reflect.X_Reflect;
import xapi.source.X_Source;
import xapi.source.read.JavaModel.IsQualified;
import xapi.time.X_Time;
import xapi.time.api.Moment;
import xapi.debug.X_Debug;
import xapi.string.X_String;
import xapi.util.api.Digester;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static xapi.dev.ui.api.UiConstants.EXTRA_FILE_NAME;
import static xapi.dev.ui.api.UiConstants.EXTRA_RESOURCE_PATH;
import static xapi.source.X_Source.rebaseMain;
import static xapi.source.X_Source.rebaseTest;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 4/1/17.
 */
public class ClasspathComponentGenerator<Ctx extends ApiGeneratorContext<Ctx>> {

    private static final Class<?> TAG = ClasspathComponentGenerator.class;
    private final String manifestOutput;
    private ExecutorService executor;

    public static String genDir(Class<?> cls) {
        final String loc = X_Reflect.getFileLoc(cls)
            .replace('\\', '/');
        return rebaseMain(rebaseTest(loc, "src/test/gen"), "src/main/gen");
    }
    private final File genDir;
    private final SimpleStack<File> backups;

    public ClasspathComponentGenerator(String myLoc) {
        this(myLoc, "build/xapi-gen/manifest.xapi");
    }
    public ClasspathComponentGenerator(String myLoc, String manifestOutput) {
        if (new File(manifestOutput).getParentFile().isDirectory()) {
            this.manifestOutput = manifestOutput;
        } else {
            File f = new File(".", manifestOutput).getAbsoluteFile();
            if (!f.getParentFile().isDirectory()) {
                boolean result = f.getParentFile().mkdirs();
                if (!result) {
                    X_Log.warn(ClasspathComponentGenerator.class,
                        "Could not create ", f.getParentFile(), " will not correctly output manifest to ", manifestOutput);
                }
            }
            this.manifestOutput = f.getAbsolutePath();
        }
        backups = new SimpleStack<>();
        this.genDir = new File(myLoc);
        if (genDir.isDirectory()) {
            clearGenDir(genDir);
        } else if (!genDir.mkdirs()) {
            throw new IllegalStateException("Unable to create dir " + myLoc);
        }
    }

    private void clearGenDir(File myLoc) {
        // Moves all files found to have a .backup suffix;
        // when we are finished, we will revisit all files,
        // and either delete the backup, or put it back, depending if the generator created a new file or not
        X_File.getAllFiles(myLoc.getAbsolutePath())
            .forEach(file->{
                if (file.endsWith(".backup")) {
                    new File(file).delete();
                    return;
                }
                File backup = new File(file+".backup");
                boolean result;
                if (backup.exists()) {
                    result = backup.delete();
                    assert result : "Unable to remove backup file " + file;
                }
                final File original = new File(file);
                result = original.renameTo(backup);
                assert result : "Unable to move file " + file;
                backups.add(backup);
            });
    }


    public <T> void generateComponents(UiGeneratorService<T> service) {
        boolean success = false;
        try {
            int generated = doGenerateComponents(service);
            success = generated > 0;
        } finally {
            final boolean delete = success;
            backups.forAll(backup -> {
                File original = new File(backup.getParent(), backup.getName().replace(".backup", ""));
                boolean result;
                // TODO: a mark/sweep of some kind?
                if (delete || original.exists()) {
                    result = backup.delete();
                    if (!result) {
                        X_Log.trace(ClasspathComponentGenerator.class, "Unable to cleanup backup file " + backup);
                    }
                } else {
                    result = backup.renameTo(original);
                    if (!result) {
                        X_Log.error(ClasspathComponentGenerator.class, "Unable to restore backup file " + backup);
                    }
                }
            });
        }
    }
    private <T> int doGenerateComponents(UiGeneratorService<T> service) {

        final Moment start = X_Time.now();
        int totalSize = 0;
        X_Log.trace(TAG, "Output dir ", genDir.getAbsolutePath());
        // find all .xapi files in our search package
        final FileBasedSourceHelper<T> sources = new FileBasedSourceHelper<>(genDir::getAbsolutePath, genDir::getAbsolutePath);
        ClasspathScanner scanner = new ClasspathScannerDefault();
        scanner.matchResource("(.*)[.]xapi");
        configure(scanner);

        Moment now = X_Time.now();
        // runs classloader scan; can be very slow if you do not specify a search package.
        final ClasspathResourceMap results = scan(scanner, getClassLoader());
        X_Log.trace(TAG, "Classpath scan returned in ", Lazy.deferred1(()->X_Time.difference(now)));
        ChainBuilder<UiContainerExpr> tagDefinitions = Chain.startChain();
        ChainBuilder<UiContainerExpr> apiDefinitions = Chain.startChain();

        for (StringDataResource xapiFile : results.getAllResources()) {
            X_Log.trace(TAG, "Processing " , xapiFile.getResourceName());
            String content = xapiFile.readAll();
            final UiContainerExpr parsed;
            try {
                parsed = JavaParser.parseUiContainer(xapiFile.getResourceName(), content);
            } catch (ParseException|Error e) {
                if (content.startsWith("<define-")) {
                    int line = AstOracle.findLine(e);
                    final String link = X_Source.pathToLogLink(xapiFile.getResourceName(),line);
                    throw new RuntimeException(link + " parser error", e);
                }
                continue;
            }
            if (!isAllowed(xapiFile, parsed)) {
                continue;
            }
            boolean defineTag = parsed.getName().startsWith("define-tag");
            boolean defineApi = parsed.getName().startsWith("define-api");
            if (defineTag || defineApi) {
                String pkg = xapiFile.getResourceName();
                int ind = pkg.lastIndexOf('/');
                String type = pkg.substring(ind+1).replace(".xapi", "");
                if (ind == -1) {
                    // xapi file in root of directory. We will stuff that here:
                    pkg = defaultPackage();
                } else {
                    pkg = pkg.substring(0, ind).replace('/', '.');
                }
                pkg = normalizePackage(pkg);

                // No api generator context available at this time.
                // TODO: refactor so we can pass in our own ctx.
                // We have a tag definition; lets create a component!
                parsed.addExtra(EXTRA_RESOURCE_PATH, pkg);
                parsed.addExtra(UiConstants.EXTRA_FILE_NAME, type);
                if (defineTag) {
                    tagDefinitions.add(parsed);
                } else  {
                    //noinspection ConstantConditions
                    assert defineApi : "if you addde more conditions to enter this block of code, " +
                        "you need to add another `else if` block here";
                    apiDefinitions.add(parsed);
                }
            }
        }

        final In1Out1<UiContainerExpr, IsQualified> typeResolver = ui->new IsQualified(
            ASTHelper.extractStringValue(
                ui.getAttribute("package")
                    .ifAbsentSupply(UiAttrExpr::of, "package", ui.<String>getExtra(EXTRA_RESOURCE_PATH))
                    .getExpression()
            ),
            ASTHelper.extractStringValue(
                ui.getAttribute("type")
                    .ifAbsentSupply(UiAttrExpr::of, "type", ui.<String>getExtra(EXTRA_FILE_NAME))
                    .getExpression()
            )
        );

        final MappedIterable<GeneratedApi> apis = service.generateApis(
            sources,
            typeResolver,
            apiDefinitions.toArray(UiContainerExpr.class)
        );
        // first, generate apis.  This lets one api file add imports / metadata for all generated components.
        int size = 0;
        for (GeneratedApi generated : apis) {
            size ++;
            X_Log.info(
                "Generated ", generated.getQualifiedName(),
                X_Source.pathToLogLink(
                    (generated.isBaseResolved() ? generated.getBase() : generated.getApi()).getQualifiedName()
                .replace('.', '/') + ".java", 30)
            );
        }
        final Moment apisDone = X_Time.now();
        X_Log.info(TAG, "Generated " + size + " apis in ", X_Time.difference(start, apisDone), " from ", apiDefinitions.size(), "definitions");
        totalSize += size;

        final MappedIterable<GeneratedUiComponent> result = service.generateComponents(
            sources,
            typeResolver,
            tagDefinitions.toArray(UiContainerExpr.class)
        );
        size = 0;
        for (GeneratedUiComponent generated : result) {
            size++;

            X_Log.info(

                "Generated ", generated.getTagName(),
                X_Source.pathToLogLink(generated.getImpls().first().getQualifiedName()
                .replace('.', '/') + ".java", 30)
            );
            X_Log.trace(TAG,
                generated.getImpls().firstMaybe()
                .mapNullSafe(GeneratedUiImplementation::toSource)
                .ifAbsentReturn("No impl for " + generated));

            final In2<GeneratedApi, GeneratedUiComponent> spy = GeneratedApi::spyComponent;
            apis.forAll(spy, generated);
        }
        totalSize += size;

        apis.forAll(GeneratedApi::finish);

        results.stop();
        scanner.shutdown();
        if (executor != null) {
            executor.shutdownNow();
        }
        X_Log.info(TAG, "Generated " + size + " components in ", X_Time.difference(apisDone), " from ", tagDefinitions.size(), "definitions");

        writeManifest(apis, result);

        return totalSize;
    }

    private void writeManifest(MappedIterable<GeneratedApi> apis, MappedIterable<GeneratedUiComponent> components) {
        if (apis.isEmpty() && components.isEmpty()) {
            return;
        }
        DomBuffer d = new DomBuffer().setNewLine(true);

        final DomBuffer generated = d.makeTag("generated");
        DomBuffer out = generated.makeTag("apis");
        for (GeneratedApi api : apis) {
            final DomBuffer item = out.makeTag("api").allowAbbreviation(true);
            item.setAttribute("name", api.getQualifiedName());
            final Digester digest = X_Inject.instance(Digester.class);
            byte[] hash = digest.digest(api.getAst().toSource().getBytes());
            item.setAttribute("hash", digest.toString(hash));
        }
        out = generated.makeTag("components");
        for (GeneratedUiComponent component : components) {
            final DomBuffer item = out.makeTag("component").allowAbbreviation(true);
            item.setAttribute("name", component.getQualifiedName());
            item.setAttribute("tagName", component.getTagName());
            // TODO: etc, as needed...  for now, we really just want this file to serve
            // as a placeholder for gradle to track whether anything meaningful has changed (so the file hashes are useful, at least).
            // This will allow us to register jobs to do more work based on whether this file has changed...
            final Digester digest = X_Inject.instance(Digester.class);
            byte[] hash = digest.digest(component.getAst().toSource().getBytes());
            item.setAttribute("hash", digest.toString(hash));
        }

        String src = d.toSource();
        try {
            X_IO.drain(new FileOutputStream(manifestOutput), X_IO.toStreamUtf8(src));
        } catch (IOException e) {
            X_Log.error(ClasspathComponentGenerator.class, "Unable to save manifest", src, e);
            throw new UncheckedIOException(e);
        }
    }

    protected boolean isAllowed(StringDataResource xapiFile, UiContainerExpr parsed) {
        return true;
    }

    /**
     * Allow subclasses to force-override packages
     */
    protected String normalizePackage(String pkg) {
        return pkg;
    }

    private ClassLoader getClassLoader() {
        final ClassLoader cl = getClass().getClassLoader();
        final URL[] urls = X_Dev.getUrls(cl);
        StringTo<URL> xapiFolders = X_Collect.newStringMapInsertionOrdered(URL.class);
        final URL[] extras = overrideClasspathUrls(urls);
        for (URL url : X_Fu.firstNotEmpty(extras, urls)) {
            String path = url.getPath().replace('\\', '/');
            final String xapiPath;
            X_Log.debug(TAG, "Considering", path);
            xapiPath = X_Source.rebaseMain(X_Source.rebaseTest(path, "src/test/xapi"), "src/main/xapi");
            if (path.equals(xapiPath)) {
                continue;
            }
            try {
                File f = new File(xapiPath);
                if (f.isDirectory()) {
                    xapiFolders.put(xapiPath, f.toURI().toURL());
                } else {
                    X_Log.debug(TAG, "Skipping non-directory", f);

                }
            } catch (MalformedURLException e) {
                throw X_Debug.rethrow(e);
            }
        }
        if (!xapiFolders.isEmpty()) {
            X_Log.trace(TAG, "Using resolved classpath", xapiFolders.forEachValue().join("\n", "\n", "\n"));
            return new URLClassLoader(xapiFolders.forEachValue().toArray(URL[]::new), cl);
        }

        return cl;
    }

    protected URL[] overrideClasspathUrls(URL[] urls) {
        return urls;
    }

    protected void configure(ClasspathScanner scanner) {
        String search = searchPackage();
        if (X_String.isNotEmptyTrimmed(search)) {
            X_Log.trace(TAG, "Using search package scope: " + search);
            scanner.scanPackage(search);
        } else {
            X_Log.warn(TAG, "Using unbounded classloader search because you did not specify a searchPackage()",
                "This operation could be very slow / expensive (enable xapi.log.level=TRACE to see how long).");
        }

    }

    protected ClasspathResourceMap scan(ClasspathScanner scanner, ClassLoader classLoader) {
        try {
            if (executor == null || executor.isShutdown()) {
                executor = Executors.newCachedThreadPool();
            }
            return scanner.scan(classLoader, executor).call();
        } catch (Exception e) {
            throw X_Debug.rethrow(e);
        }
    }

    protected String searchPackage() {
        return "xapi.ui";
    }

    protected String defaultPackage() {
        return "xapi.ui.generated";
    }

}
