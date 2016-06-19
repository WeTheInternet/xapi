package xapi.jre.ui.demo;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.expr.UiContainerExpr;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import xapi.dev.ui.GeneratedComponentMetadata;
import xapi.dev.ui.UiGeneratorService;
import xapi.io.X_IO;
import xapi.jre.ui.impl.UiGeneratorServiceJavaFx;
import xapi.source.X_Source;

import java.io.InputStream;

/**
 * Created by james on 6/7/16.
 */
public class JreUiDemo extends Application {

    public static void main(String ... args) {
        Application.launch(JreUiDemo.class);
    }

    @Override
    public void start(Stage stage) throws Exception {
      String template;
      try (InputStream in = JreUiDemo.class.getResourceAsStream("Demo.xapi")) {
          template = X_IO.toStringUtf8(in);
      }
      final UiContainerExpr container = JavaParser.parseUiContainer(template);
      final UiGeneratorService uiService = new UiGeneratorServiceJavaFx();
      final GeneratedComponentMetadata generated = uiService.generateComponent(
          getClass().getPackage().getName(),
          X_Source.classToEnclosedSourceName(getClass()),
          container
      );

        System.out.println(generated.getSourceBuilder());


        stage.setTitle("Hi");
        Button b = new Button("Clicky");
        b.setOnAction(e->stage.setTitle("Clicked"));
        Region region = new Region();
//        region.getScene()
        stage.setScene(new Scene(b, 100, 100));
        stage.show();
    }
}
