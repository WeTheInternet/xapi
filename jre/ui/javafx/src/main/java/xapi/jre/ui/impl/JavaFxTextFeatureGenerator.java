package xapi.jre.ui.impl;

import com.github.javaparser.ASTHelper;
import com.github.javaparser.ast.expr.UiAttrExpr;
import xapi.dev.source.MethodBuffer;
import xapi.dev.ui.ComponentBuffer;
import xapi.dev.ui.ContainerMetadata;
import xapi.dev.ui.UiComponentGenerator;
import xapi.dev.ui.UiFeatureGenerator;
import xapi.dev.ui.UiGeneratorTools;
import xapi.dev.ui.UiVisitScope;
import xapi.dev.ui.UiVisitScope.ScopeType;
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
    String text = ASTHelper.extractAttrValue(n);
    String panel = parent.peekPanelName();
    final MethodBuffer mb = parent.getMethod(panel);
    mb.println(panel +".setText(\"" + X_Source.escape(text) + "\");");
    return new UiVisitScope(ScopeType.CONTAINER).setVisitChildren(false);
  }
}
