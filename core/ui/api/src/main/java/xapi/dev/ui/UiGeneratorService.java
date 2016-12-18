package xapi.dev.ui;

import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.dev.gen.SourceHelper;
import xapi.dev.ui.ContainerMetadata.MetadataRoot;
import xapi.fu.Do;
import xapi.source.read.JavaModel.IsQualified;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 5/4/16.
 */
public interface UiGeneratorService <Raw> {

    ComponentBuffer runPhase(String id, ComponentBuffer component);

    UiComponentGenerator getComponentGenerator(UiContainerExpr container, ContainerMetadata metadata);

    UiFeatureGenerator getFeatureGenerator(UiAttrExpr container, UiComponentGenerator componentGenerator);

    ContainerMetadata createMetadata(MetadataRoot root, UiContainerExpr n);

    void overwriteResource(String path, String fileName, String source, Raw hints);

    void overwriteSource(String path, String fileName, String source, Raw hints);

    UiGeneratorVisitor createVisitor(ContainerMetadata metadata);

    ComponentBuffer initialize(SourceHelper<Raw> service, IsQualified type, UiContainerExpr container);

    UiGeneratorTools tools();

    void finish();

    void onFinish(Do ondone);
}
