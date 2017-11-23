package xapi.demo.jre;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.expr.UiContainerExpr;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import xapi.dev.impl.DevApp;
import xapi.dev.ui.api.UiGeneratorPlatform;
import xapi.fu.Do;
import xapi.fu.In1;
import xapi.fu.Mutable;
import xapi.fu.Pointer;
import xapi.fu.X_Fu;
import xapi.javac.dev.api.CompilerService;
import xapi.javac.dev.model.CompilerSettings.ImplicitMode;
import xapi.jre.ui.impl.SelfCompilingJavaFxApp;
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
public class XapiLangAdmin extends DevApp {

    static{
        X_Properties.setProperty(UiGeneratorPlatform.SYSTEM_PROP_IGNORE_PLATFORM, UiGeneratorPlatform.PLATFORM_WEB_COMPONENT);
    }

    public static final ScreenLogger logger = ScreenLogger.getLogger();
    private XapiVertxServer server;

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
}
