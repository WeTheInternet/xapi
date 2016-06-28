package xapi.jre.ui.impl;

import com.github.javaparser.ast.expr.UiContainerExpr;
import javafx.scene.control.Button;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.dev.ui.ContainerMetadata;
import xapi.dev.ui.UiComponentGenerator;
import xapi.dev.ui.UiGeneratorService;

/**
 * Created by james on 6/17/16.
 */
public class JavaFxButtonComponentGenerator extends UiComponentGenerator {

  public JavaFxButtonComponentGenerator() {

  }

  @Override
  public boolean startVisit(
        UiGeneratorService service, ContainerMetadata me, UiContainerExpr n
  ) {

    String parentName = me.peekPanelName();
    final MethodBuffer mb = me.getMethod(parentName);
    final SourceBuilder<?> out = me.getSourceBuilder();
    String button = out.addImport(Button.class);
    String ref = me.getRefName("button");
    mb.println(button + " " + ref + " = new " + button + "();");
    mb.println(parentName + ".getChildren().add(" + ref + ");");
    me.pushPanelName(ref);
    me.saveMethod(ref, mb);
    return super.startVisit(service, me, n);
  }

  @Override
  public void endVisit(
        UiGeneratorService service, ContainerMetadata me, UiContainerExpr n
  ) {
    me.popPanelName();
    me.removeMethod("b");
    super.endVisit(service, me, n);
  }
}
