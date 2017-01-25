package xapi.dev.ui;

import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.annotation.inject.InstanceDefault;
import xapi.dev.api.ApiGeneratorContext;

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
        return state.componentFactory.io(container, metadata);
    }

    @Override
    public UiFeatureGenerator getFeatureGenerator(UiAttrExpr container, UiComponentGenerator componentGenerator) {
        return state.featureFactory.io(container, componentGenerator);
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
