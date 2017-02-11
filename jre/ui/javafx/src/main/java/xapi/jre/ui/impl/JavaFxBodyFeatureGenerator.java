package xapi.jre.ui.impl;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.dev.ui.api.ComponentBuffer;
import xapi.dev.ui.api.ContainerMetadata;
import xapi.dev.ui.api.UiComponentGenerator;
import xapi.dev.ui.api.UiFeatureGenerator;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.dev.ui.api.UiVisitScope;
import xapi.dev.ui.api.UiVisitScope.ScopeType;

import static xapi.dev.ui.api.UiVisitScope.visitScope;

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
