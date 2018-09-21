package xapi.dev.ui.api;

import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import com.github.javaparser.ast.type.ReferenceType;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.gen.SourceHelper;
import xapi.dev.ui.impl.AbstractUiImplementationGenerator;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.dev.ui.impl.UiGeneratorVisitor;
import xapi.fu.Do;
import xapi.fu.In1Out1;
import xapi.fu.itr.MappedIterable;
import xapi.log.api.LogLevel;
import xapi.source.read.JavaModel.IsQualified;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 5/4/16.
 */
public interface UiGeneratorService <Raw> {

    void runPhase(ComponentBuffer component, String id);

    UiComponentGenerator getComponentGenerator(UiContainerExpr container, ContainerMetadata metadata);

    UiFeatureGenerator getFeatureGenerator(UiAttrExpr container, UiComponentGenerator componentGenerator);

    ContainerMetadata createMetadata(MetadataRoot root, UiContainerExpr n);

    String overwriteResource(String path, String fileName, String source, Raw hints);

    void overwriteSource(String path, String fileName, String source, Raw hints);

    UiGeneratorVisitor createVisitor(ContainerMetadata metadata, ComponentBuffer buffer);

    ComponentBuffer initialize(SourceHelper<Raw> service, IsQualified type, UiContainerExpr container);

    UiGeneratorTools tools();

    void finish(MappedIterable<ComponentBuffer> itr, String cleanup);

    /**
     * Run a task later during generation; these tasks are run in sets of priority,
     * from Integer.MIN_VALUE to MAX_VALUE, with all tasks in the same priority
     * running in the same pass.
     *
     * This allows you to specify a clear order of operations,
     * where you might want a final task to run at Integer.MAX_VALUE,
     * with all job-preparation tasks in Integer.MIN_VALUE to -1,
     * all default jobs running in group 0, and 1 to Integer.MAX_VALUE
     * as actual pieces of work to perform.
     *
     * @param priority
     * @param ondone
     */
    void onFinish(int priority, Do ondone);

    MappedIterable<GeneratedUiComponent> allComponents();

    MappedIterable<GeneratedUiComponent> generateComponents(SourceHelper<Raw> sources,
                                                            In1Out1<UiContainerExpr, IsQualified> type,
                                                            UiContainerExpr ... parsed);

    GeneratedUiDefinition getComponentDefinition(ApiGeneratorContext<?> ctx, String name);
    ComponentBuffer getComponent(ApiGeneratorContext<?> ctx, String name);

    ReferenceType getDefaultComponentType(AbstractUiImplementationGenerator impl, GeneratedUiComponent component);

    boolean isStrict();

    void setStrict(boolean strict);

    LogLevel getLevel();

    void setLevel(LogLevel level);
}
