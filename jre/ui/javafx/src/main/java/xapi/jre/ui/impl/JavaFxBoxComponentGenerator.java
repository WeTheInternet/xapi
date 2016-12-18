package xapi.jre.ui.impl;

import com.github.javaparser.ASTHelper;
import com.github.javaparser.ast.expr.UiContainerExpr;
import javafx.scene.layout.VBox;
import xapi.dev.source.MethodBuffer;
import xapi.dev.ui.ContainerMetadata;
import xapi.dev.ui.UiComponentGenerator;
import xapi.dev.ui.UiGeneratorTools;
import xapi.dev.ui.UiVisitScope;

/**
 * Created by james on 6/17/16.
 */
public class JavaFxBoxComponentGenerator extends UiComponentGenerator {

  @Override
  public UiVisitScope startVisit(
        UiGeneratorTools service, ContainerMetadata me, UiContainerExpr n
  ) {

    String parentName = me.peekPanelName();
    final MethodBuffer mb = me.getMethod(parentName);
    String container = n.getAttribute("type")
          .mapDeferred(ASTHelper::extractAttrValue)
          .getIfNull(VBox.class.getCanonicalName());
    container = mb.addImport(container);

    String ref = me.getRefName("box");
    mb.println(container + " " + ref + " = new " + container + "();");

    mb.println(parentName + ".getChildren().add(" + ref + ");");
    me.pushPanelName(ref);
    me.saveMethod(ref, mb);
    return super.startVisit(service, me, n);
  }

  @Override
  public void endVisit(
        UiGeneratorTools service, ContainerMetadata me, UiContainerExpr n,
        UiVisitScope scope
  ) {
    String panel = me.popPanelName();
    me.removeMethod(panel);
    super.endVisit(service, me, n, scope);
  }
}
