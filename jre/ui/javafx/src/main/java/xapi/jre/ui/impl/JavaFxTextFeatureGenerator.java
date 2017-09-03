package xapi.jre.ui.impl;

import com.github.javaparser.ASTHelper;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.UiAttrExpr;
import xapi.dev.source.MethodBuffer;
import xapi.dev.ui.api.ComponentBuffer;
import xapi.dev.ui.api.ContainerMetadata;
import xapi.dev.ui.api.UiComponentGenerator;
import xapi.dev.ui.api.UiFeatureGenerator;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.dev.ui.api.UiVisitScope;
import xapi.dev.ui.api.UiVisitScope.ScopeType;
import xapi.source.X_Source;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/19/16.
 */
public class JavaFxTextFeatureGenerator extends UiFeatureGenerator {

  @Override
  public UiVisitScope startVisit(
      UiGeneratorTools service,
      UiComponentGenerator generator,
      ComponentBuffer source,
      ContainerMetadata parent,
      UiAttrExpr n
  ) {
    boolean isMethod = n.getExpression() instanceof MethodCallExpr;
    String text = isMethod ? service.resolveString(source.getRoot().getContext(), n.getExpression()) : ASTHelper.extractAttrValue(n);
    String panel = parent.peekPanelName();
    final MethodBuffer mb = parent.getMethod(panel);
    mb.print(panel +".setText(")
      .print(isMethod ? text : "\"" + X_Source.escape(text) + "\"")
      .println(");");
    return new UiVisitScope(ScopeType.CONTAINER).setVisitChildren(false);
  }
}
