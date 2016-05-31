package xapi.dev.ui;

import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 5/4/16.
 */
public interface UiGeneratorService {

  UiComponentGenerator getComponentGenerator(UiContainerExpr container, GeneratedComponentMetadata metadta);

  UiFeatureGenerator getFeatureGenerator(UiAttrExpr container, UiComponentGenerator componentGenerator);

  default GeneratedComponentMetadata createMetadata(UiContainerExpr n) {
    return new GeneratedComponentMetadata(n);
  }
}
