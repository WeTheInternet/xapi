package xapi.jre.ui.impl;

import com.github.javaparser.ast.expr.UiContainerExpr;
import javafx.scene.control.Button;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.dev.ui.ComponentBuffer;
import xapi.dev.ui.ContainerMetadata;
import xapi.dev.ui.UiComponentGenerator;
import xapi.dev.ui.UiGeneratorTools;
import xapi.dev.ui.UiVisitScope;

/**
 * Created by james on 6/17/16.
 */
public class JavaFxButtonComponentGenerator extends UiComponentGenerator {

  public JavaFxButtonComponentGenerator() {

  }

  @Override
  public UiVisitScope startVisit(
      UiGeneratorTools service, ComponentBuffer source, ContainerMetadata me, UiContainerExpr n,
      UiGenerateMode mode
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
    return super.startVisit(service, source, me, n, mode);
  }

  @Override
  public void endVisit(
        UiGeneratorTools service, ContainerMetadata me, UiContainerExpr n,
        UiVisitScope scope
  ) {
    String pop = me.popPanelName();
    me.removeMethod(pop);
    super.endVisit(service, me, n, scope);
  }
}
