package xapi.jre.ui.demo;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.expr.UiContainerExpr;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import xapi.dev.source.SourceBuilder;
import xapi.dev.ui.GeneratedComponentMetadata;
import xapi.dev.ui.UiGeneratorService;
import xapi.fu.Out2;
import xapi.fu.Pointer;
import xapi.inject.X_Inject;
import xapi.io.X_IO;
import xapi.javac.dev.api.CompilerService;
import xapi.javac.dev.model.CompilerSettings;
import xapi.jre.ui.impl.UiGeneratorServiceJavaFx;

import static xapi.source.X_Source.classToEnclosedSourceName;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Created by james on 6/7/16.
 */
public class JreUiDemo extends Application {

  private Stage stage;

  public static void main(String... args) {
    Application.launch(JreUiDemo.class);
  }

  @Override
  public void start(Stage stage) throws Exception {
    this.stage = stage;

    int a = -23;
    int b = +a;
    System.out.println(b);
    String template;
    try (InputStream in = JreUiDemo.class.getResourceAsStream("Demo.xapi")) {
      template = X_IO.toStringUtf8(in);
    }
    final UiContainerExpr container = JavaParser.parseUiContainer(template);
    final UiGeneratorService uiService = new UiGeneratorServiceJavaFx();
    final String pkg = getClass().getPackage().getName();
    final String simpleName = classToEnclosedSourceName(getClass());
    final GeneratedComponentMetadata generated = uiService.generateComponent(
        pkg, simpleName, container);

    final SourceBuilder<?> builder = generated.getSourceBuilder();
    System.out.println(builder);

    CompilerService compiler = X_Inject.singleton(CompilerService.class);
    final CompilerSettings settings = compiler.defaultSettings()
        .setTest(true)
        .setClearGenerateDirectory(false);
    settings.resetGenerateDirectory();
    File dir = new File(settings.getGenerateDirectory());
    File into = new File(dir, builder.getSourceFileName());
    boolean dirs = into.getParentFile().getCanonicalFile().mkdirs();
    assert dirs : "Failed to create output directory " + into;
    InputStream input = X_IO.toStreamUtf8(builder.toSource());
    try (
        OutputStream output = new FileOutputStream(into)
    ) {
      X_IO.drain(output, input);
    }

    Out2<Integer, URL> result = compiler.compileFiles(settings,
        builder.getSourceFileName());
    assert 0 == result.out1().intValue() : "Javac failed...";

    URLClassLoader cl = new URLClassLoader(new URL[]{result.out2()}, Thread.currentThread().getContextClassLoader());
    Pointer<Parent> value = Pointer.pointer();
    Thread runIn = new Thread(()->{
      try {
        final Class<?> cls = cl.loadClass(builder.getQualifiedName());
        Object o = cls.newInstance();
        Object test = cls.getMethod("io", JreUiDemo.class).invoke(o, JreUiDemo.this);
        value.in((Parent) test);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    runIn.setContextClassLoader(cl);
    runIn.start();
    runIn.join();


    System.out.println(value.out1());

    stage.setTitle("Hi");
    stage.setScene(new Scene(value.out1(), 200, 200));
    stage.show();
  }

  public Stage getStage() {
    return stage;
  }
}
