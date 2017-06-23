package xapi.dev.ui.impl;

import xapi.dev.ui.api.ComponentBuffer;
import xapi.dev.ui.api.ContainerMetadata;
import xapi.dev.ui.api.GeneratedUiComponent;
import xapi.dev.ui.api.GeneratedUiImplementation;
import xapi.platform.Platform;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/7/17.
 */
public class ApiOnlyGenerator extends AbstractUiImplementationGenerator<ApiOnlyGeneratorContext> {

    @Override
    protected void initializeComponent(GeneratedUiComponent result, ContainerMetadata metadata) {
        if (result.addImplementationFactory(Platform.class, GeneratedApiOnlyComponent::new)) {
            super.initializeComponent(result, metadata);
        }
    }

    @Override
    public GeneratedUiImplementation getImpl(GeneratedUiComponent component) {
        return component.getImpls()
            .filterInstanceOf(GeneratedApiOnlyComponent.class)
            .firstMaybe()
            .getOrThrow(()->new IllegalStateException("Did not have a GeneratedApiOnlyComponent."));
    }

    @Override
    protected String getImplPrefix() {
        return "Stub";
    }
}
