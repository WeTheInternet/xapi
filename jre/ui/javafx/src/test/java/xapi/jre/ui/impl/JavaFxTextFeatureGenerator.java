package xapi.jre.ui.impl;

import com.github.javaparser.ASTHelper;
import com.github.javaparser.ast.expr.UiAttrExpr;
import xapi.dev.source.MethodBuffer;
import xapi.dev.ui.GeneratedComponentMetadata;
import xapi.dev.ui.UiComponentGenerator;
import xapi.dev.ui.UiFeatureGenerator;
import xapi.dev.ui.UiGeneratorService;
import xapi.source.X_Source;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/19/16.
 */
public class JavaFxTextFeatureGenerator extends UiFeatureGenerator {

  @Override
  public boolean startVisit(
      UiGeneratorService service, UiComponentGenerator generator, GeneratedComponentMetadata parent, UiAttrExpr n
  ) {
    String text = ASTHelper.extractAttrValue(n);
    String panel = parent.peekPanelName();
    final MethodBuffer mb = parent.getMethod(panel);
    mb.println(panel +".setText(\"" + X_Source.escape(text) + "\");");
    return false;
  }
}
