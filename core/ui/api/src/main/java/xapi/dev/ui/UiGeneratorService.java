package xapi.dev.ui;

import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.dev.ui.GeneratedComponentMetadata.MetadataRoot;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 5/4/16.
 */
public interface UiGeneratorService {

  GeneratedComponentMetadata generateComponent(String pkgName, String className, UiContainerExpr expr);

  UiComponentGenerator getComponentGenerator(UiContainerExpr container, GeneratedComponentMetadata metadta);

  UiFeatureGenerator getFeatureGenerator(UiAttrExpr container, UiComponentGenerator componentGenerator);

  default GeneratedComponentMetadata createMetadata(MetadataRoot root, UiContainerExpr n) {
    final GeneratedComponentMetadata component = new GeneratedComponentMetadata(n);
    component.setRoot(new MetadataRoot());

    return component;
  }
}
