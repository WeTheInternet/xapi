package xapi.dev.ui;

import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.dev.ui.ContainerMetadata.MetadataRoot;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 5/4/16.
 */
public interface UiGeneratorService {

  ContainerMetadata generateComponent(String pkgName, String className, UiContainerExpr expr);

  UiComponentGenerator getComponentGenerator(UiContainerExpr container, ContainerMetadata metadta);

  UiFeatureGenerator getFeatureGenerator(UiAttrExpr container, UiComponentGenerator componentGenerator);

  default ContainerMetadata createMetadata(MetadataRoot root, UiContainerExpr n) {
    final ContainerMetadata component = new ContainerMetadata(n);
    component.setRoot(new MetadataRoot());

    return component;
  }
}
