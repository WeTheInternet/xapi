package xapi.dev.ui.api;

import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.dev.gen.SourceHelper;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.dev.ui.api.ContainerMetadata.MetadataRoot;
import xapi.dev.ui.impl.UiGeneratorVisitor;
import xapi.fu.Do;
import xapi.fu.In1Out1;
import xapi.fu.MappedIterable;
import xapi.source.read.JavaModel.IsQualified;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 5/4/16.
 */
public interface UiGeneratorService <Raw> {

    ComponentBuffer runPhase(ComponentBuffer component, String id);

    UiComponentGenerator getComponentGenerator(UiContainerExpr container, ContainerMetadata metadata);

    UiFeatureGenerator getFeatureGenerator(UiAttrExpr container, UiComponentGenerator componentGenerator);

    ContainerMetadata createMetadata(MetadataRoot root, UiContainerExpr n);

    void overwriteResource(String path, String fileName, String source, Raw hints);

    void overwriteSource(String path, String fileName, String source, Raw hints);

    UiGeneratorVisitor createVisitor(ContainerMetadata metadata, ComponentBuffer buffer);

    ComponentBuffer initialize(SourceHelper<Raw> service, IsQualified type, UiContainerExpr container);

    UiGeneratorTools tools();

    void finish();

    void onFinish(Do ondone);

    MappedIterable<GeneratedUiComponent> allComponents();

    MappedIterable<GeneratedUiComponent> generateComponents(SourceHelper<Raw> sources,
                                                            In1Out1<UiContainerExpr, IsQualified> type,
                                                            UiContainerExpr ... parsed);

    ComponentBuffer getBuffer(String name);
}
