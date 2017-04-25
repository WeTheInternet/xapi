package xapi.dev.ui.impl;

import com.github.javaparser.ASTHelper;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.dev.X_Dev;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.gen.FileBasedSourceHelper;
import xapi.dev.resource.impl.StringDataResource;
import xapi.dev.scanner.api.ClasspathScanner;
import xapi.dev.scanner.impl.ClasspathResourceMap;
import xapi.dev.scanner.impl.ClasspathScannerDefault;
import xapi.dev.ui.api.GeneratedUiComponent;
import xapi.dev.ui.api.GeneratedUiImplementation;
import xapi.dev.ui.api.UiConstants;
import xapi.dev.ui.api.UiGeneratorService;
import xapi.file.X_File;
import xapi.fu.Lazy;
import xapi.fu.MappedIterable;
import xapi.fu.X_Fu;
import xapi.fu.iterate.Chain;
import xapi.fu.iterate.ChainBuilder;
import xapi.log.X_Log;
import xapi.reflect.X_Reflect;
import xapi.source.read.JavaModel.IsQualified;
import xapi.time.X_Time;
import xapi.time.api.Moment;
import xapi.util.X_Debug;
import xapi.util.X_String;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static xapi.dev.ui.api.UiConstants.EXTRA_FILE_NAME;
import static xapi.dev.ui.api.UiConstants.EXTRA_RESOURCE_PATH;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 4/1/17.
 */
public class ClasspathComponentGenerator<Ctx extends ApiGeneratorContext<Ctx>> {

    private ExecutorService executor;

    public static String genDir(Class<?> cls) {
        return X_Reflect.getFileLoc(cls)
            .replace('\\', '/')
            .replace("target/classes", "src/main/gen")
            .replace("target/test-classes", "src/test/gen");
    }

    public ClasspathComponentGenerator(String myLoc) {
        this.genDir = new File(myLoc);
        if (genDir.isDirectory()) {
            clearGenDir(genDir);
        }
        if (!genDir.mkdirs()) {
            throw new IllegalStateException("Unable to create dir " + myLoc);
        }
    }

    private void clearGenDir(File myLoc) {
            // TODO: either delete all, or mark all files stale.
            // Avoiding timestamp churn would be best,
            // so a mark-sweep is preferred...
            // but, for now, we just want clean builds every time.
        X_File.deepDelete(myLoc.getAbsolutePath());
    }

    private final File genDir;

    public <T> void generateComponents(UiGeneratorService<T> service) {

        X_Log.trace(getClass(), "Scanning ", genDir.getAbsolutePath());
        // find all .xapi files in our search package
        final FileBasedSourceHelper<T> sources = new FileBasedSourceHelper<>(genDir::getAbsolutePath);
        ClasspathScanner scanner = new ClasspathScannerDefault();
        scanner.matchResource("(.*)[.]xapi");
        configure(scanner);

        Moment now = X_Time.now();
        // runs classloader scan; can be very slow if you do not specify a search package.
        final ClasspathResourceMap results = scan(scanner, getClassLoader());
        X_Log.trace(getClass(), "Classpath scan returned in ", Lazy.deferred1(()->X_Time.difference(now)));
        ChainBuilder<UiContainerExpr> tagDefinitions = Chain.startChain();
        for (StringDataResource xapiFile : results.getAllResources()) {
            X_Log.trace(getClass(), "Processing " , xapiFile.getResourceName());
            String content = xapiFile.readAll();
            final UiContainerExpr parsed;
            try {
                parsed = JavaParser.parseUiContainer(content);
            } catch (ParseException|Error e) {
                if (content.startsWith("<define-tag")) {
                    throw new RuntimeException(
                        "Failure generating component for " + xapiFile.getResourceName(), e);
                }
                continue;
            }
            if (parsed.getName().endsWith("define-tag")) {
                String pkg = xapiFile.getResourceName();
                int ind = pkg.lastIndexOf('/');
                String type = pkg.substring(ind+1).replace(".xapi", "");
                if (ind == -1) {
                    // xapi file in root of directory. We will stuff that here:
                    pkg = defaultPackage();
                } else {
                    pkg = pkg.substring(0, ind).replace('/', '.');
                }

                // No api generator context available at this time.
                // TODO: refactor so we can pass in our own ctx.
                // We have a tag definition; lets create a component!
                parsed.addExtra(EXTRA_RESOURCE_PATH, pkg);
                parsed.addExtra(UiConstants.EXTRA_FILE_NAME, type);
                tagDefinitions.add(parsed);
            }
        }
        final MappedIterable<GeneratedUiComponent> result = service.generateComponents(
            sources,
            ui->new IsQualified(
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
            ),
            tagDefinitions.toArray(UiContainerExpr[]::new)
        );

        for (GeneratedUiComponent generated : result) {
            X_Log.trace(getClass(), "Generated ", generated.getTagName(), generated.getImpls().firstMaybe()
                .mapNullSafe(GeneratedUiImplementation::toSource)
                .ifAbsentReturn("No impl for " + generated));
        }

        results.stop();
        scanner.shutdown();
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private ClassLoader getClassLoader() {
        final ClassLoader cl = getClass().getClassLoader();
        final URL[] urls = X_Dev.getUrls(cl);
        StringTo<URL> xapiFolders = X_Collect.newStringMapInsertionOrdered(URL.class);
        for (URL url : X_Fu.concat(urls, getExtraUrls())) {
            String path = url.getPath().replace('\\', '/');
            final String xapiPath;
            X_Log.debug(getClass(), "Considering", path);
            if (path.endsWith("target/classes/")) {
                xapiPath = path.replace("target/classes/", "src/main/xapi/");
            }else if (path.endsWith("target/test-classes/")) {
                xapiPath = path.replace("target/test-classes/", "src/test/xapi/");
            } else {
                continue;
            }
            try {
                File f = new File(xapiPath);
                if (f.isDirectory()) {
                    xapiFolders.put(xapiPath, f.toURI().toURL());
                } else {
                    X_Log.debug(getClass(), "Skipping non-directory", f);

                }
            } catch (MalformedURLException e) {
                throw X_Debug.rethrow(e);
            }
        }
        if (!xapiFolders.isEmpty()) {
            X_Log.trace(getClass(), "Using resolved classpath", xapiFolders.forEachValue().join("\n", "\n", "\n"));
            return new URLClassLoader(xapiFolders.forEachValue().toArray(URL[]::new), cl);
        }

        return cl;
    }

    protected URL[] getExtraUrls() {
        return new URL[0];
    }

    protected void configure(ClasspathScanner scanner) {
        String search = searchPackage();
        if (X_String.isNotEmptyTrimmed(search)) {
            scanner.scanPackage(search);
        } else {
            X_Log.warn(getClass(), "Using unbounded classloader search because you did not specify a searchPackage()",
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
