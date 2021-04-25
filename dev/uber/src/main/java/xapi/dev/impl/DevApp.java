package xapi.dev.impl;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.expr.UiContainerExpr;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import xapi.dev.X_Dev;
import xapi.dev.api.MavenLoader;
import xapi.dev.opts.DevOpts;
import xapi.fu.Do.DoUnsafe;
import xapi.fu.Lazy;
import xapi.fu.Mutable;
import xapi.fu.Out1;
import xapi.fu.X_Fu;
import xapi.jre.ui.impl.SelfCompilingJavaFxApp;
import xapi.log.X_Log;
import xapi.mvn.X_Maven;
import xapi.mvn.api.MvnDependency;
import xapi.process.X_Process;
import xapi.ui.api.Ui;
import xapi.constants.X_Namespace;
import xapi.string.X_String;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/11/17.
 */
@Ui("<import file=`DevApp.xapi` />")
public class DevApp extends Application implements SelfCompilingJavaFxApp, MavenLoader {

    static {
        System.setProperty(X_Namespace.PROPERTY_MAVEN_UNRESOLVABLE, ".*/xapi-(gwt|dev)/.*");
    }

    private static final DevOpts opts = new DevOpts();
    private String appName;
    private int port = 13337;
    private final Lazy<DevAppServer> appServer = Lazy.deferred1(this::createAppServer);

    protected DevAppServer createAppServer() {
        return new DevAppServer();
    }

    public static void main(String ... args) {

        Mutable<Boolean> success = new Mutable<>();
        final String[] remaining = opts.processArgs(success.ignore2(), args);
        if (success.out1()) {
            if (opts.isHeadless()) {
                new DevApp().startHeadless(remaining);
            } else {
                Application.launch(DevApp.class, remaining);
            }
        } else {
            System.err.println("DevApp failed to launch;");
            System.err.println("If there are no useful log messages,");
            System.err.println("try running with -Dxapi.log.level=DEBUG -Dxapi.debug=true");
        }
    }

    protected void startHeadless(String ... remaining) {
        opts.processArgs(remaining); // in case someone calls us externally
        if (!opts.isServerless()) {
            startServer();
        }
    }

    @Override
    public void start(Stage stage) throws Exception {

        stage.setTitle("Xapi Admin");
        final Parent parent = compileAndGetJavaFxUi();

        stage.setScene(new Scene(parent, 600, 400));
        stage.show();
        if (!opts.isServerless()) {
            startServer();
        }
    }

    public void restartServer() {
        stopServer(this::startServer);
    }

    public void stopServer(DoUnsafe onDone) {
        if (appServer.isResolved()) {
            appServer.out1().shutdown(onDone);
        }
    }

    public void startServer() {
        // We want to assemble a classloader that has xapi-gwt uber jar loaded onto it
        URLClassLoader loader = createClasspath(opts);
        X_Process.runInClassloader(loader, appServer
            .map(DevAppServer::doStartServer, getAppName() + "Server", getPort())
            .ignoreOut1().unsafe());
    }

    protected URLClassLoader createClasspath(DevOpts opts) {
        final URL[] urls =
             downloadDependencyNow(getDependency("xapi-gwt"))
            .map(X_Dev::ensureProtocol)
            .filterNull()
            .mapUnsafe(URL::new)
            .toArray(URL[]::new);
        @SuppressWarnings("UnnecessaryLocalVariable") // nice for debugging
        final URLClassLoader cp = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
        return cp;
    }

    public synchronized String getAppName() {
        if (appName == null) {
            appName = computeAppName();
        }
        return appName;
    }

    protected String computeAppName() {
        final Ui uiAnno = getClass().getAnnotation(Ui.class);
        if (uiAnno == null) {
            throw new IllegalStateException(getClass() + " must have a @Ui annotation!");
        }
        String source = X_String.join("", uiAnno.value());
        try {
            final UiContainerExpr xapi = JavaParser.parseUiContainer(getClass().getCanonicalName(), source);
            if (xapi.getName().startsWith("import")) {
                return xapi.getAttributeNotNull("file")
                    .getStringExpression(false)
                    .replace(".xapi", "");
            }
        } catch (ParseException e) {
            X_Log.error(DevApp.class, "Error checking app name");
        }
        return getClass().getCanonicalName();
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public int getPort() {
        return port;
    }

    @Override
    public Out1<Iterable<String>> downloadDependency(MvnDependency dependency) {
        return X_Maven.downloadDependencies(dependency)
            .map(X_Fu::weaken);
    }
}
