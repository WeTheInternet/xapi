package xapi.jre.ui.impl;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.dev.ui.ComponentBuffer;
import xapi.dev.ui.ContainerMetadata;
import xapi.dev.ui.UiComponentGenerator;
import xapi.dev.ui.UiFeatureGenerator;
import xapi.dev.ui.UiGeneratorTools;
import xapi.dev.ui.UiVisitScope;
import xapi.dev.ui.UiVisitScope.ScopeType;

import static xapi.dev.ui.UiVisitScope.visitScope;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/19/16.
 */
public class JavaFxBodyFeatureGenerator extends UiFeatureGenerator {

  @Override
  public UiVisitScope startVisit(
      UiGeneratorTools service,
      UiComponentGenerator generator,
      ComponentBuffer source,
      ContainerMetadata parent,
      UiAttrExpr n
  ) {
    final Expression expr = n.getExpression();
    if (!(expr instanceof UiContainerExpr)) {
      throw new IllegalArgumentException("Body feature does not support values of type " + expr.getClass());
    }
    UiContainerExpr container = (UiContainerExpr) expr;

    return visitScope(ScopeType.CONTAINER, false);
  }
}
