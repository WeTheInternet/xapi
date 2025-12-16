package xapi.jre.ui.impl;

import net.wti.lang.parser.ASTHelper;
import net.wti.lang.parser.ast.body.Parameter;
import net.wti.lang.parser.ast.expr.Expression;
import net.wti.lang.parser.ast.expr.LambdaExpr;
import net.wti.lang.parser.ast.expr.MethodReferenceExpr;
import net.wti.lang.parser.ast.expr.UiAttrExpr;
import net.wti.lang.parser.ast.plugin.Transformer;
import net.wti.lang.parser.ast.stmt.ExpressionStmt;
import net.wti.lang.parser.ast.stmt.Statement;
import xapi.dev.source.MethodBuffer;
import xapi.dev.ui.api.ComponentBuffer;
import xapi.dev.ui.api.ContainerMetadata;
import xapi.dev.ui.api.UiComponentGenerator;
import xapi.dev.ui.api.UiFeatureGenerator;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.dev.ui.api.UiVisitScope;
import xapi.dev.ui.api.UiVisitScope.ScopeType;

import static xapi.dev.ui.api.UiVisitScope.visitScope;

import java.util.Arrays;
import java.util.List;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/19/16.
 */
public class JavaFxActionFeatureGenerator extends UiFeatureGenerator {

  @Override
  public UiVisitScope startVisit(
      UiGeneratorTools service,
      UiComponentGenerator generator,
      ComponentBuffer source,
      ContainerMetadata me,
      UiAttrExpr n
  ) {
    final Expression orig = n.getExpression();
    final Expression expr = service.resolveVar(source.getRoot().getContext(), orig);
    MethodBuffer into = me.getParentMethod();
    String target = me.peekPanelName();
    into.println(target + ".setOnAction(")
        .indent();

    final Transformer transformer = generator.getTransformer(service, source.getRoot().getContext());
    if (expr instanceof LambdaExpr || expr instanceof MethodReferenceExpr) {
      into.printlns(expr.toSource(transformer));
    } else {
      final Statement statement = new ExpressionStmt(expr);
      final List<Parameter> params = Arrays.asList(
            ASTHelper.createParameter(me.newVarName("e"))
      );
      LambdaExpr lambda = new LambdaExpr(params, statement, false);
      into.printlns(lambda.toSource(transformer));
    }
    into
        .outdent()
        .println(");");

    return visitScope(ScopeType.FEATURE, false);
  }
}
