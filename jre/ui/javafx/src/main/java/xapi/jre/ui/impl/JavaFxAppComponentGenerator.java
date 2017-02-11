package xapi.jre.ui.impl;

import com.github.javaparser.ASTHelper;
import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import javafx.scene.layout.VBox;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.dev.ui.api.ComponentBuffer;
import xapi.dev.ui.api.ContainerMetadata;
import xapi.dev.ui.api.UiComponentGenerator;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.dev.ui.api.UiVisitScope;
import xapi.fu.In1Out1;
import xapi.fu.Maybe;

/**
 * Created by james on 6/17/16.
 */
public class JavaFxAppComponentGenerator extends UiComponentGenerator {

  public JavaFxAppComponentGenerator() {

  }

  @Override
  public UiVisitScope startVisit(
      UiGeneratorTools service, ComponentBuffer source, ContainerMetadata me, UiContainerExpr n,
      UiGenerateMode mode
  ) {

    SourceBuilder<?> out = me.getSourceBuilder();
    final Maybe<UiAttrExpr> type = n.getAttribute("type");
    String container = type
        .mapNullSafe(ASTHelper::extractAttrValue)
        .ifAbsentSupply(VBox.class::getName);
    container = out.addImport(container);
    String binder = out.addImport(In1Out1.class);

    String controller = out.addImport(me.getControllerType());

    final ClassBuffer cb = out.getClassBuffer();
    cb.addInterface(binder + "<" + controller + ", " + container + ">");
    // Even though we are the mapping for <app />,
    // we are actually going to bind to a JavaFX stage;
    // this is because we want to be callable within other
    // JavaFX code, and Application.launch may only be called once!

    String refName = me.getRefName();

    final MethodBuffer method = cb.createMethod("public " + container + " io(" + controller + " " + refName + ")");
    String varName = me.newVarName("root");
    method.println(container + " " + varName + " = new " + container + "();");

    me.saveMethod(varName, method);
    me.pushPanelName(varName);

    return super.startVisit(service, source, me, n, mode);
  }

  @Override
  public void endVisit(
        UiGeneratorTools service, ContainerMetadata me, UiContainerExpr n,
        UiVisitScope scope
  ) {
    String panelName = me.popPanelName();
    me.getMethod(panelName)
      .returnValue(panelName);
    super.endVisit(service, me, n, scope);
  }
}
