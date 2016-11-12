package xapi.server.gen;

import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.annotation.inject.InstanceOverride;
import xapi.dev.api.ApiGeneratorTools;
import xapi.dev.gen.SourceHelper;
import xapi.dev.source.SourceBuilder;
import xapi.dev.ui.*;
import xapi.fu.iterate.Chain;
import xapi.fu.Out2;
import xapi.inject.X_Inject;
import xapi.io.X_IO;
import xapi.javac.dev.api.CompilerService;
import xapi.javac.dev.model.CompilerSettings;
import xapi.log.X_Log;
import xapi.model.X_Model;
import xapi.server.api.Classpath;
import xapi.server.api.WebApp;
import xapi.source.read.JavaModel.IsQualified;
import xapi.ui.api.PhaseMap;
import xapi.ui.api.PhaseMap.PhaseNode;
import xapi.util.X_Util;

import static xapi.util.X_Util.firstNotNull;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/8/16.
 */
@InstanceOverride(implFor=UiGeneratorService.class)
public class WebAppGenerator extends AbstractUiGeneratorService <WebAppGeneratorContext, WebAppGeneratorContext> implements ApiGeneratorTools<WebAppGeneratorContext> {

    private WebAppGeneratorContext ctx;

    public WebApp generateWebApp(String name, UiContainerExpr container) {
        ctx = new WebAppGeneratorContext();
        UiGeneratorService<WebAppGeneratorContext> service = createUiGenerator();

        CompilerService compiler = X_Inject.singleton(CompilerService.class);
        final CompilerSettings settings = compiler.defaultSettings();
        settings.setVerbose(true);
        settings.setTest(true);

        String genDir = settings.getGenerateDirectory();
        ctx.setGeneratorDirectory(genDir);
        final SourceHelper<WebAppGeneratorContext> tools = createSourceHelper(ctx);
        final IsQualified type = new IsQualified(name);
        ComponentBuffer buffer = service.initialize(tools, type, container);

        PhaseMap<String> phaseMap = PhaseMap.withDefaults(new LinkedHashSet<>());
        for (PhaseNode<String> phase : phaseMap.forEachNode()) {
            buffer = service.runPhase(phase.getId(), buffer);
        }

//        final UiGeneratorVisitor visitor = service.createVisitor(buffer.getRoot());
//        visitor.visit(container, service.tools());
        final SourceBuilder<ContainerMetadata> source = buffer.getBinder();
        String src = source.toSource();
        X_Log.info(getClass(), src, "Generated... " + src);
        try {
            Path dir = Paths.get(genDir);
            if (!source.getPackage().isEmpty()) {
                dir = dir.resolve(source.getPackage().replace('.', File.separatorChar));
            }
            Files.createDirectories(dir);
            final Path file = dir.resolve(source.getSimpleName()+".java");
            Files.write(file, src.getBytes(X_Util.defaultCharset()));
            final Out2<Integer, URL> task = compiler.compileFiles(settings, file.toString());
            assert task.out1() == 0 : "Failed compilation; error code: " + task.out1();
            final URL cp = task.out2();

            WebApp model = newModel(type, cp, src);
            return model;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
        };
    }

    @Override
    protected WebAppGeneratorContext getHints() {
        return ctx;
    }

    protected UiGeneratorService<WebAppGeneratorContext> createUiGenerator() {
        return this;
    }

    protected WebApp newModel(IsQualified type, URL cp, String src) {
        WebApp app = X_Model.create(WebApp.class);
        app.setSource(src);
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
                        Out2.out2Immutable("web-app", new WebAppComponentGenerator()),
                        Out2.out2Immutable("classpath", new ClasspathComponentGenerator()),
                        Out2.out2Immutable("gwtc", new GwtcComponentGenerator())
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
    protected UiFeatureGenerator createCssFeatureGenerator() {
        return new CssFeatureGenerator();
    }
}
