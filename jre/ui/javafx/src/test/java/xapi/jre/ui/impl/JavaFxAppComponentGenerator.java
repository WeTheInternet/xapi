package xapi.jre.ui.impl;

import com.github.javaparser.ast.expr.UiContainerExpr;
import javafx.stage.Stage;
import xapi.dev.source.SourceBuilder;
import xapi.dev.ui.GeneratedComponentMetadata;
import xapi.dev.ui.UiComponentGenerator;
import xapi.dev.ui.UiGeneratorService;

/**
 * Created by james on 6/17/16.
 */
public class JavaFxAppComponentGenerator extends UiComponentGenerator {

  public JavaFxAppComponentGenerator() {

  }

  @Override
  public boolean visitSuper(
      UiGeneratorService service, GeneratedComponentMetadata me, UiContainerExpr n
  ) {
    SourceBuilder<?> out = me.getSourceBuilder();
//
//    if (out == null) {
//      out = java();
//      me.setSourceBuilder(out);
//    }
    if (!out.isDefined()) {

    }

    String stageCls = out.addImport(Stage.class);

    // Even though we are the mapping for <app />,
    // we are actually going to bind to a JavaFX stage;
    // this is because we want to be callable within other
    // JavaFX code, and Application.launch may only be called once!


    return super.visitSuper(service, me, n);
  }
}
