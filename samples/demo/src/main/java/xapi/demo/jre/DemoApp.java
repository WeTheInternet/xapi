package xapi.demo.jre;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import xapi.fu.Pointer;
import xapi.javac.dev.api.CompilerService;
import xapi.log.X_Log;
import xapi.ui.api.Ui;

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
@Ui("<import file=`Demo.xapi` />")
public class DemoApp extends Application {

    public static class DoNotExit extends RuntimeException {}

    public static void main(String ... args) {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            System.err.println("Thread " + t.getName() + " died");
            die(e);
        });
        Throwable fail = null;
        try {
            Application.launch(DemoApp.class, args);
        } catch (Throwable t){
            fail = t;
            throw t;
        } finally {
            die(fail);
        }

    }

    private static void die(Throwable e) {
        if (e != null) {
            e.printStackTrace(System.err);
            final Path path = Paths.get("logErr");
            System.out.print("Demo app failure");
            System.out.println(Files.exists(path) ? " check file:" + path.toAbsolutePath() : "");
        }
        System.out.println();// make sure to clear the buffer so our goodbye is seen as a whole line of output.
        System.out.println("goodbye");
        if (e instanceof DoNotExit) {
            return;
        }
        System.exit(e == null ? 0 : 1);
    }

    @Override
    public void start(Stage stage) throws Exception {
        CompilerService compiler = singleton(CompilerService.class);
        Pointer<Parent> value = Pointer.pointer();
        final String generatedName = getClass().getPackage().getName() + ".JavaFx" + getClass().getSimpleName() + "Component";
        compiler.startCompile(DemoApp.class)
            .withSettings(s->
                s.setClearGenerateDirectory(false)
                    .resetGenerateDirectory()
            )
            .compileAndRun((cl, cls) -> {
                    final Class<?> generated = cl.loadClass(generatedName);
                    Object o = generated.newInstance();
                    Object test = generated.getMethod("io", DemoApp.class).invoke(o, DemoApp.this);
                    value.in((Parent)test);
                }
            );

        stage.setTitle("XApi @ GwtCon 2017");
        stage.setScene(new Scene(value.out1(), 600, 400));
        stage.show();
    }

    protected void startServer() {
        X_Log.info(DemoApp.class, "Starting server");


    }
}
