package xapi.dev.ui;

import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.dev.processor.AnnotationTools;
import xapi.dev.ui.ContainerMetadata.MetadataRoot;
import xapi.ui.api.Ui;

import javax.lang.model.element.TypeElement;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 5/4/16.
 */
public interface UiGeneratorService {

    ComponentBuffer runPhase(String id, ComponentBuffer component);

    UiComponentGenerator getComponentGenerator(UiContainerExpr container, ContainerMetadata metadata);

    UiFeatureGenerator getFeatureGenerator(UiAttrExpr container, UiComponentGenerator componentGenerator);

    ContainerMetadata createMetadata(MetadataRoot root, UiContainerExpr n);

    UiGeneratorVisitor createVisitor(ContainerMetadata metadata);

    ComponentBuffer initialize(AnnotationTools service, TypeElement type, Ui ui, UiContainerExpr container);
}
