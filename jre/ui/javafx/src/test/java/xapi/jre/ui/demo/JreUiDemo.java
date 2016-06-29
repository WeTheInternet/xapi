package xapi.jre.ui.demo;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import xapi.fu.Pointer;
import xapi.javac.dev.api.CompilerService;
import xapi.ui.api.Ui;

import static xapi.inject.X_Inject.singleton;

/**
 * Created by james on 6/7/16.
 */
@Ui("<import file=`Demo.xapi` />")
public class JreUiDemo extends Application {

    private Stage stage;

    public static void main(String... args) {
        Application.launch(JreUiDemo.class);
    }

    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;

        CompilerService compiler = singleton(CompilerService.class);
        Pointer<Parent> value = Pointer.pointer();
        final String generatedName = getClass().getPackage().getName() + ".JavaFxComponent0_" + getClass().getSimpleName();
        compiler.processAnnotationsAndRun(JreUiDemo.class,
              (cl, cls) -> {
                  final Class<?> generated = cl.loadClass(generatedName);
                  Object o = generated.newInstance();
                  Object test = generated.getMethod("io", JreUiDemo.class).invoke(o, JreUiDemo.this);
                  value.in((Parent) test);
              }
        );

        stage.setTitle("Hi");
        stage.setScene(new Scene(value.out1(), 200, 200));
        stage.show();
    }

    public Stage getStage() {
        return stage;
    }

}
