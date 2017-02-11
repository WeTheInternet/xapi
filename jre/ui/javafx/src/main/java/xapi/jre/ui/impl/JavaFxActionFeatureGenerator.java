package xapi.jre.ui.impl;

import com.github.javaparser.ASTHelper;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
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
    final Expression expr = n.getExpression();
    MethodBuffer into = me.getParentMethod();
    String target = me.peekPanelName();
    into.println(target + ".setOnAction(")
        .indent();

    if (expr instanceof LambdaExpr || expr instanceof MethodReferenceExpr) {
      into.printlns(expr.toSource(generator.getTransformer()));
    } else {
      final Statement statement = new ExpressionStmt(expr);
      final List<Parameter> params = Arrays.asList(
            ASTHelper.createParameter(me.newVarName("e"))
      );
      LambdaExpr lambda = new LambdaExpr(params, statement, false);
      into.printlns(lambda.toSource(generator.getTransformer()));
    }
    into
        .outdent()
        .println(");");

    return visitScope(ScopeType.CONTAINER, false);
  }
}
