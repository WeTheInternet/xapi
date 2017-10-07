package xapi.demo.jre;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.expr.UiContainerExpr;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import xapi.dev.ui.api.UiGeneratorPlatform;
import xapi.fu.In1;
import xapi.fu.Mutable;
import xapi.fu.Pointer;
import xapi.javac.dev.api.CompilerService;
import xapi.javac.dev.model.CompilerSettings.ImplicitMode;
import xapi.log.X_Log;
import xapi.log.api.LogLevel;
import xapi.server.api.ServerManager;
import xapi.server.api.WebApp;
import xapi.server.api.XapiServer;
import xapi.server.gen.VertxWebAppGenerator;
import xapi.server.vertx.XapiVertxServer;
import xapi.ui.api.Ui;
import xapi.util.X_Properties;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static xapi.inject.X_Inject.singleton;

/**
 * An all-in-one(der) demo application of xapi features.
 *
 * Starts a xapi jre gui, which runs a xapi server
 * that exposes a xapi client application
 * to showcase at GwtCon 2017.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 9/2/17.
 */
@Ui("<import file=`XapiLangAdmin.xapi` />")
public class XapiLangAdmin extends Application implements ServerManager<XapiVertxServer>{

    static{
        X_Properties.setProperty(UiGeneratorPlatform.SYSTEM_PROP_IGNORE_PLATFORM, UiGeneratorPlatform.PLATFORM_WEB_COMPONENT);
    }

    public static final ScreenLogger logger = ScreenLogger.getLogger();

    public static void main(String ... args) {

        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            e.printStackTrace();
            logger.log(LogLevel.ERROR, XapiLangAdmin.class, "Uncaught exception", e);
        });

        Throwable fail = null;
        try {
            Application.launch(XapiLangAdmin.class, args);
        } catch (Throwable t){
            fail = t;
            throw t;
        } finally {
            if (fail != null) {
                logger.log(LogLevel.ERROR, XapiLangAdmin.class, "Application launch failed", fail);
            }
        }

    }

    @Override
    public void start(Stage stage) throws Exception {
        CompilerService compiler = singleton(CompilerService.class);
        Pointer<Parent> value = Pointer.pointer();
        final String generatedName = getClass().getPackage().getName() + ".JavaFx" + getClass().getSimpleName() + "Component";
        compiler.startCompile(XapiLangAdmin.class)
            .withSettings(s->
                s.setClearGenerateDirectory(false)
                 .resetGenerateDirectory()
            )
            .compileAndRun((cl, cls) -> {
                    final Class<?> generated = cl.loadClass(generatedName);
                    Object o = generated.newInstance();
                    Object test = generated.getMethod("io", XapiLangAdmin.class).invoke(o, XapiLangAdmin.this);
                    value.in((Parent)test);
                }
            );

        stage.setTitle("Xapi Demo");
        stage.setScene(new Scene(value.out1(), 600, 400));
        stage.show();
        startServer();
    }

    public void startServer() {
        X_Log.info(XapiLangAdmin.class, "Starting server");
        VertxWebAppGenerator generator = new VertxWebAppGenerator(s->{
            // we'll leave the /gwt folder for compiled client stuff...
            s.setGenerateDirectory(s.getGenerateDirectory().replace("/gwt", "/annotations"));
            s.setImplicitMode(ImplicitMode.CLASS);
            return s;
        });
        final UiContainerExpr ui;
        try {
            ui = JavaParser.parseXapi(getClass().getResource("XapiLangServer.xapi").openStream());
        } catch (Exception e) {
            X_Log.error(XapiLangAdmin.class, "Could not load server xapi file", e);
            throw new RuntimeException(e);
        }

        final Mutable<In1<XapiServer<?>>> install = new Mutable<>();
        final WebApp app = generator.generateWebApp("XapiLangServer", ui, install);
        app.setPort(13337);
        webApps().put("XapiLangServer", app);
        XapiVertxServer server = getServer("XapiLangServer");
        if (isRunning("XapiLangServer")) {
            install.out1().in(server);
        } else {
            startup("XapiLangServer", server, done->{
                install.out1().in(done);
                server.start();
            });
        }
    }

    @Override
    public XapiVertxServer newServer(String name, WebApp classpath) {
        return new XapiVertxServer(classpath);
    }
}
