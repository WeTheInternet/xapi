package xapi.dev.ui;

import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.annotation.inject.InstanceDefault;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.ui.api.ComponentBuffer;
import xapi.dev.ui.api.ContainerMetadata;
import xapi.dev.ui.api.UiComponentGenerator;
import xapi.dev.ui.api.UiFeatureGenerator;
import xapi.dev.ui.api.UiGeneratorService;
import xapi.dev.ui.impl.AbstractUiGeneratorService;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/26/16.
 */
@InstanceDefault(implFor = UiGeneratorService.class)
public class UiGeneratorServiceDefault <Raw, Ctx extends ApiGeneratorContext<Ctx>> extends AbstractUiGeneratorService<Raw, Ctx> {

    public UiGeneratorServiceDefault() {
        resetFactories();
    }

    @Override
    public UiComponentGenerator getComponentGenerator(UiContainerExpr container, ContainerMetadata metadata) {
        return state.getComponentFactory().io(container, metadata);
    }

    @Override
    public UiFeatureGenerator getFeatureGenerator(UiAttrExpr container, UiComponentGenerator componentGenerator) {
        return state.getFeatureFactory().io(container, componentGenerator);
    }

    @Override
    protected UiFeatureGenerator createCssFeatureGenerator() {
        return new CssFeatureGenerator();
    }

    @Override
    protected UiFeatureGenerator createDataFeatureGenerator() {
        return new DataFeatureGenerator();
    }

    @Override
    protected UiFeatureGenerator createModelFeatureGenerator() {
        return new ModelFeatureGenerator();
    }

    protected ComponentBuffer peekIntegration(ComponentBuffer component) {
        return component;
    }

    protected ComponentBuffer runBinding(ComponentBuffer component) {
        return component;
    }

    protected ComponentBuffer runCustomPhase(String id, ComponentBuffer component) {
        return component;
    }

}
