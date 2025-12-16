package xapi.jre.ui.impl;

import net.wti.lang.parser.ASTHelper;
import net.wti.lang.parser.ast.expr.UiContainerExpr;
import javafx.scene.layout.VBox;
import xapi.dev.source.MethodBuffer;
import xapi.dev.ui.api.ComponentBuffer;
import xapi.dev.ui.api.ContainerMetadata;
import xapi.dev.ui.api.UiComponentGenerator;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.dev.ui.api.UiVisitScope;

/**
 * Created by james on 6/17/16.
 */
public class JavaFxBoxComponentGenerator extends UiComponentGenerator {

  @Override
  public UiVisitScope startVisit(
      UiGeneratorTools service, ComponentBuffer source, ContainerMetadata me, UiContainerExpr n,
      UiGenerateMode mode
  ) {

    String parentName = me.peekPanelName();
    final MethodBuffer mb = me.getMethod(parentName);
    String container = n.getAttribute("type")
          .mapNullSafe(ASTHelper::extractAttrValue)
          .mapNullSafe(v->v.contains(".") ? v : "javafx.scene.layout." + v)
          .ifAbsentSupply(VBox.class::getCanonicalName);
    container = mb.addImport(container);

    String ref = me.getRefName("box");
    mb.println(container + " " + ref + " = new " + container + "();");
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
    String panel = me.popPanelName();
    me.removeMethod(panel);
    super.endVisit(service, me, n, scope);
  }
}
