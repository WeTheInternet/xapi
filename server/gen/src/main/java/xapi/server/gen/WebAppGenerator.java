package xapi.server.gen;

import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.annotation.inject.InstanceOverride;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.api.ApiGeneratorTools;
import xapi.dev.gen.SourceHelper;
import xapi.dev.source.SourceBuilder;
import xapi.dev.ui.CssFeatureGenerator;
import xapi.dev.ui.DataFeatureGenerator;
import xapi.dev.ui.ModelFeatureGenerator;
import xapi.dev.ui.api.*;
import xapi.dev.ui.impl.AbstractUiGeneratorService;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.fu.In1;
import xapi.fu.In1Out1;
import xapi.fu.itr.MappedIterable;
import xapi.fu.Maybe;
import xapi.fu.Out2;
import xapi.fu.itr.Chain;
import xapi.fu.itr.ChainBuilder;
import xapi.fu.itr.SingletonIterator;
import xapi.fu.itr.SizedIterable;
import xapi.inject.X_Inject;
import xapi.io.X_IO;
import xapi.javac.dev.api.CompilerService;
import xapi.javac.dev.model.CompilerSettings;
import xapi.javac.dev.model.CompilerSettings.ImplicitMode;
import xapi.log.X_Log;
import xapi.model.X_Model;
import xapi.dev.api.Classpath;
import xapi.server.api.WebApp;
import xapi.server.api.XapiServer;
import xapi.server.api.XapiServerPlugin;
import xapi.source.X_Source;
import xapi.source.read.JavaModel.IsQualified;
import xapi.ui.api.PhaseMap;
import xapi.ui.api.PhaseMap.PhaseNode;
import xapi.ui.api.UiPhase;
import xapi.util.X_Namespace;
import xapi.util.X_String;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;

import static xapi.fu.Out2.out2Immutable;
import static xapi.util.X_Util.firstNotNull;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/8/16.
 */
@InstanceOverride(implFor=UiGeneratorService.class)
public class WebAppGenerator extends AbstractUiGeneratorService<WebAppGeneratorContext, WebAppGeneratorContext> implements ApiGeneratorTools<WebAppGeneratorContext> {

    public static final int PRIORITY_RESOLVE_CLASSPATHS = 100_000;
    public static final int PRIORITY_RESOLVE_TEMPLATES = 100_100;
    public static final int PRIORITY_RESOLVE_GWT = 100_200;
    public static final int PRIORITY_RESOLVE_ROUTES = 100_300;

    private final In1Out1<CompilerSettings, CompilerSettings> settingsFactory;
    private WebAppGeneratorContext ctx;
    private boolean test;
    private final StringTo<GeneratedClasspathInfo> classpathInfo;

    public WebAppGenerator(In1Out1<CompilerSettings, CompilerSettings> settings) {
        this.settingsFactory = settings;
        classpathInfo = classpathMap();
    }

    public WebAppGenerator() {
        this.settingsFactory = s->{
            s.setVerbose(true);
            s.setTest(isTest());
            s.setImplicitMode(ImplicitMode.CLASS);
            return s;
        };
        classpathInfo = classpathMap();
    }

    protected StringTo<GeneratedClasspathInfo> classpathMap() {
        return X_Collect.newStringMapInsertionOrdered(GeneratedClasspathInfo.class);
    }

    @Override
    protected WebAppGeneratorContext contextFor(
        IsQualified type, UiContainerExpr container
    ) {
        if (ctx == null) {
            ctx = new WebAppGeneratorContext();
        }
        return ctx;
    }

    protected boolean isTest() {
        return test;
    }

    public WebAppGenerator setTest(boolean test) {
        this.test = test;
        return this;
    }

    protected String getDefaultPackage() {
        return "xapi.generated.web";
    }

    public WebApp generateWebApp(String name, UiContainerExpr container, In1<In1<XapiServer<?>>> callback) {
        return generateWebApp(name, SingletonIterator.singleItem(container), callback);
    }

    public WebApp generateWebApp(String name, MappedIterable<UiContainerExpr> containers, In1<In1<XapiServer<?>>> callback) {
        ctx = new WebAppGeneratorContext();
        UiGeneratorService<WebAppGeneratorContext> service = createUiGenerator();

        CompilerService compiler = X_Inject.singleton(CompilerService.class);
        CompilerSettings settings = compiler.defaultSettings();
        settings = prepareSettings(name, containers, settings);

        String genDir = settings.getGenerateDirectory();
        ctx.setGeneratorDirectory(genDir);
        ctx.setOutputDirectory(settings.getOutputDirectory());

        final SourceHelper<WebAppGeneratorContext> tools = createSourceHelper(ctx);
        final IsQualified type;
        if (name.indexOf('.') == -1) {
            type = new IsQualified(getDefaultPackage(), name);
        } else {
            final String pkg = X_Source.toPackage(name);
            type = new IsQualified(pkg, name.replace(pkg+".", ""));
        }

        final SizedIterable<ComponentBuffer> buffers = containers.map(expr -> service.initialize(
            tools,
            type,
            expr
        ))
            .caching()
            .counted();
        // handles multi-file compiles sanely
        // We could expand on this to have a method that only generates the phases of each buffer, but delays running #finish()
        // but, for now, our use cases are simple enough to handle all-at-once compilation.
        PhaseMap<String> phaseMap = PhaseMap.withDefaults(new LinkedHashSet<>());
        for (PhaseNode<String> phase : phaseMap.forEachNode()) {
            for (ComponentBuffer buffer : buffers) {
                service.runPhase(buffer, phase.getId());
            }

        }
        finish(buffers, UiPhase.CLEANUP);

        final GeneratedUiImplementation example = getPreferredImplType();
        Path dir = Paths.get(genDir);

        final MappedIterable<String> qualifiedNames = buffers.map(buffer -> buffer
            .getGeneratedComponent()
            .getBestImpl(example)
            .getSource()
            .getQualifiedName())
            .cached();
        final String[] files =
            (qualifiedNames
                .append(
                    buffers.map(ComponentBuffer::getGeneratedComponent)
                        .map(GeneratedUiComponent::getBase)
                        .map(GeneratedUiLayer::getQualifiedName)
                )
            )
            .map(file->file.replace('.', File.separatorChar) + ".java")
            .map(dir::resolve)
            .map(Path::toString)
            .toArray(String[]::new);


        final Out2<Integer, URL> task = compiler.compileFiles(settings, files);
        assert task.out1() == 0 : "Failed compilation; error code: " + task.out1();
        final URL cp = task.out2();

        WebApp model = newModel(cp);

        model.setBaseSource(buffers.first().getGeneratedComponent().getBase().getSource().toSource());
        // TODO: save this model somewhere so that other generated instances can reference our metadata / output
        model.setContentRoot(settings.getOutputDirectory());
        X_Log.info(WebAppGenerator.class, "Using content root", settings.getOutputDirectory());
        // initialize model using the class we just generated...
        callback.in(server->{
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            final Method install;
            try {
                install = XapiServerPlugin.class.getMethod(
                    "installToServer",
                    WebApp.class
                );
            } catch (NoSuchMethodException e) {
                X_Log.error(WebAppGenerator.class, "Unable to load generated method XapiServerPlugin.installToServer for " + X_String.join("\n", files), e);
                return;
            }
            ChainBuilder<In1> callbacks = Chain.startChain();
            boolean hadError = false;
            for (String source : qualifiedNames) {
                try {
                    final Class<?> component = loader.loadClass(source);
                    final Object plugin = component.newInstance();
                    // TODO: make this an In2<XapiServer, Iterable<Throwable>>, so we can tell installed modules if other modules failed
                    // (so they can either release resource, or compensate somehow
                    final In1 result = (In1) install.invoke(plugin, model);
                    callbacks.add(result);
                } catch (ClassNotFoundException e) {
                    hadError = true;
                    X_Log.error(WebAppGenerator.class, "Unable to load generated class " + source, e);
                    X_Log.error(WebAppGenerator.class, "Unable to access constructor of " + source, e);
                } catch (InstantiationException e) {
                    hadError = true;
                    X_Log.error(WebAppGenerator.class, "Error encountered in constructor of " + source, e);
                } catch (InvocationTargetException e) {
                    hadError = true;
                    X_Log.error(WebAppGenerator.class, "Error encountered calling installToServer in " + source, e);
                } catch (IllegalAccessException e) {
                    hadError = true;
                    X_Log.error(WebAppGenerator.class, "Error encountered calling constructor of " + source, e);
                }
            }
            callbacks.removeAll(In1::in, server);

        });
        return model;
    }

    protected GeneratedUiImplementation getPreferredImplType() {
        return null;
    }

    protected CompilerSettings prepareSettings(String name, MappedIterable<UiContainerExpr> containers, CompilerSettings settings) {
        return settingsFactory.io(settings);
    }

    protected SourceHelper<WebAppGeneratorContext> createSourceHelper(WebAppGeneratorContext ctx) {
        return new SourceHelper<WebAppGeneratorContext>() {
            @Override
            public String readSource(String pkgName, String clsName, WebAppGeneratorContext hints) {
                return firstNotNull(hints, ctx)
                    .readSource(pkgName, clsName);
            }

            @Override
            public void saveSource(String pkgName, String clsName, String src, WebAppGeneratorContext hints) {
                final WebAppGeneratorContext used = firstNotNull(hints, ctx);
                final SourceBuilder<WebAppGeneratorContext> buffer = used.getOrMakeClass(pkgName, clsName, false);
                buffer.replaceSource(src);
                Path dir = Paths.get(used.getGeneratorDirectory());
                dir = dir.resolve(pkgName.replace('.', '/'));
                try {
                    final Path output = Files.createDirectories(dir).resolve(clsName + ".java");
                    X_IO.drain(Files.newOutputStream(output), X_IO.toStreamUtf8(src));
                } catch (IOException e) {
                    throw rethrow(e);
                }
            }

            @Override
            public String saveResource(
                String path, String fileName, String src, WebAppGeneratorContext hints
            ) {
                final WebAppGeneratorContext used = firstNotNull(hints, ctx);
                final SourceBuilder<WebAppGeneratorContext> buffer = used.getOrMakeClass(path, fileName, false);
                buffer.replaceSource(src);
                Path dir = Paths.get(used.getOutputDirectory());
                if (path.matches("[/]?[.][.][/]?")) {
                    warnPathWithDotDot(path, used);
                }
                if (path.startsWith("/")) {

                }
                dir = dir.resolve(path);
                try {
                    final Path output = Files.createDirectories(dir).resolve(fileName);
                    X_IO.drain(Files.newOutputStream(output), X_IO.toStreamUtf8(src));
                    return output.toAbsolutePath().toString();
                } catch (IOException e) {
                    X_Log.info(WebAppGenerator.class, "Error saving", dir, "/", fileName, e);
                    throw rethrow(e);
                }
            }

            @Override
            public Class<WebAppGeneratorContext> hintType() {
                return WebAppGeneratorContext.class;
            }

        };
    }

    protected void warnPathWithDotDot(String path, WebAppGeneratorContext ctx) {
        throw new IllegalArgumentException("Do not use .. in path names; bad path: " + path);
    }

    @Override
    protected WebAppGeneratorContext getHints() {
        return ctx;
    }

    protected UiGeneratorService<WebAppGeneratorContext> createUiGenerator() {
        return this;
    }

    protected WebApp newModel(URL cp) {
        WebApp app = X_Model.create(WebApp.class);
        final Classpath classpath = newClasspath(cp);
        app.addClasspath("root", classpath);
        return app;
    }

    protected Classpath newClasspath(URL cp) {
        Classpath classpath = X_Model.create(Classpath.class);
        classpath.getOrCreatePaths().add(cp.toExternalForm());
        return classpath;
    }

    @Override
    protected Iterable<Out2<String, UiComponentGenerator>> getComponentGenerators() {
        return
                    Chain.<Out2<String, UiComponentGenerator>>toChain(
                        out2Immutable("define-tags", tools().createTagGenerator()),
                        out2Immutable("define-tag", tools().createTagGenerator()),
                        out2Immutable("web-app", new WebAppComponentGenerator(this)),
                        out2Immutable("classpath", new UiComponentGenerator()),
                        out2Immutable("gwtc", new GwtcComponentGenerator())
                    )
                    .build();
    }

    @Override
    protected Iterable<Out2<String, UiFeatureGenerator>> getFeatureGenerators() {
        return super.getFeatureGenerators();
    }

    @Override
    protected UiFeatureGenerator createDataFeatureGenerator() {
        return new DataFeatureGenerator();
    }

    @Override
    protected UiFeatureGenerator createModelFeatureGenerator() {
        return new ModelFeatureGenerator();
    }

    @Override
    protected UiFeatureGenerator createCssFeatureGenerator() {
        return new CssFeatureGenerator();
    }

    public SizedIterable<GeneratedClasspathInfo> getClasspaths() {
        return classpathInfo.forEachValue();
    }

    public Maybe<GeneratedClasspathInfo> getClasspath(String named) {
        if (named.startsWith("$")) {
            named = named.substring(1);
        }
        return classpathInfo.getMaybeBound(named);
    }

    public void addClasspath(String name, GeneratedClasspathInfo cp) {
        final GeneratedClasspathInfo existing = classpathInfo.get(name);
        if (existing == null) {
            classpathInfo.put(name, cp);
        } else {
            classpathInfo.put(name, mergeStrategy(cp, existing));
        }
    }

    protected GeneratedClasspathInfo mergeStrategy(GeneratedClasspathInfo newCp, GeneratedClasspathInfo oldCp) {
        // Current merge strategy is to have existing item absorb new item
        oldCp.absorb(newCp);
        return oldCp;
    }

    public String getDefaultGroupId() {
        return "net.wetheinter";
    }

    public String getDefaultVersion() {
        return X_Namespace.XAPI_VERSION;
    }

    public static String getExpressionSerialized(UiAttrExpr expr, UiGeneratorTools tools, ApiGeneratorContext ctx) {
        return expr == null ? null : tools.resolveString(ctx, expr.getExpression());
    }
}
