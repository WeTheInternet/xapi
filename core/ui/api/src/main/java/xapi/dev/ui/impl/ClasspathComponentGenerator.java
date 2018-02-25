package xapi.dev.ui.impl;

import com.github.javaparser.ASTHelper;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.collect.impl.SimpleStack;
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
import xapi.source.X_Source;
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

    private static final Class<?> TAG = ClasspathComponentGenerator.class;
    private ExecutorService executor;

    public static String genDir(Class<?> cls) {
        return X_Reflect.getFileLoc(cls)
            .replace('\\', '/')
            .replace("target/classes", "src/main/gen")
            .replace("target/test-classes", "src/test/gen");
    }
    private final File genDir;
    private final SimpleStack<File> backups;

    public ClasspathComponentGenerator(String myLoc) {
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
        try {
            doGenerateComponents(service);
        } finally {
            backups.forAll(backup -> {
                File original = new File(backup.getParent(), backup.getName().replace(".backup", ""));
                boolean result;
                if (original.exists()) {
                    result = backup.delete();
                    assert result : "Unable to cleanup backup file " + backup;
                } else {
                    result = backup.renameTo(original);
                    assert result : "Unable to restore backup file " + backup;
                }
            });
        }
    }
    private <T> void doGenerateComponents(UiGeneratorService<T> service) {

        final Moment start = X_Time.now();
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
        for (StringDataResource xapiFile : results.getAllResources()) {
            X_Log.trace(TAG, "Processing " , xapiFile.getResourceName());
            String content = xapiFile.readAll();
            final UiContainerExpr parsed;
            try {
                parsed = JavaParser.parseUiContainer(content);
            } catch (ParseException|Error e) {
                if (content.startsWith("<define-tag")) {
                    throw new RuntimeException(
                        "Failure generating component for " +
                            X_Source.pathToLogLink(xapiFile.getResourceName(),
                                e instanceof ParseException ? (((ParseException) e).currentToken.beginLine) : 1)
                        , e);
                }
                continue;
            }
            if (!isAllowed(xapiFile, parsed)) {
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
                pkg = normalizePackage(pkg);

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

        int size = 0;
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
        }

        results.stop();
        scanner.shutdown();
        if (executor != null) {
            executor.shutdownNow();
        }
        X_Log.info(TAG, "Generated " + size + " components in ", X_Time.difference(start));
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
