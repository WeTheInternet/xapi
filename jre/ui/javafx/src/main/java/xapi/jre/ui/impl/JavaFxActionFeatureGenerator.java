package xapi.jre.ui.impl;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.UiAttrExpr;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.PrintBuffer;
import xapi.dev.ui.ContainerMetadata;
import xapi.dev.ui.UiComponentGenerator;
import xapi.dev.ui.UiFeatureGenerator;
import xapi.dev.ui.UiGeneratorTools;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/19/16.
 */
public class JavaFxActionFeatureGenerator extends UiFeatureGenerator {

  @Override
  public boolean startVisit(
        UiGeneratorTools service, UiComponentGenerator generator, ContainerMetadata me, UiAttrExpr n) {
    final Expression expr = n.getExpression();
    MethodBuffer into = me.getParentMethod();
    String target = me.peekPanelName();
    final PrintBuffer buffer = new PrintBuffer();
    into.addToEnd(buffer);
    buffer.println(target + ".setOnAction(")
        .indent()
        .printlns(expr.toSource(generator.getTransformer()))
        .outdent()
        .println(");");

    return false;
  }
}
