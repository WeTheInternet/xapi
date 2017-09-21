package xapi.server.gen;

import xapi.dev.ui.api.ContainerMetadata;
import xapi.dev.ui.api.UiComponentGenerator;
import xapi.dev.ui.impl.AbstractUiImplementationGenerator;
import xapi.dev.ui.api.GeneratedUiComponent;
import xapi.dev.ui.api.GeneratedUiImplementation;
import xapi.fu.Out2;
import xapi.server.vertx.api.VertxPlatform;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/23/16.
 */
public class VertxWebAppImplGenerator extends AbstractUiImplementationGenerator<WebAppGeneratorContext>{

    public VertxWebAppImplGenerator(VertxWebAppGenerator generator) {
        super();
        generator.getComponentGenerators().forEach(componentGenerators::putPair);
    }

    @Override
    protected String getImplPrefix() {
        return "Vertx";
    }

    @Override
    protected void initializeComponent(
        GeneratedUiComponent result,
        ContainerMetadata metadata
    ) {
        if (result.addImplementationFactory(VertxPlatform.class, GeneratedVertxComponent::new)) {
            super.initializeComponent(result, metadata);
        }
    }

    @Override
    public GeneratedVertxComponent getImpl(GeneratedUiComponent component) {
        for (GeneratedUiImplementation impl : component.getImpls()) {
            if (impl instanceof GeneratedVertxComponent) {
                return (GeneratedVertxComponent) impl;
            }
        }
        throw new IllegalStateException("No vertx impl found in " + component.getImpls());
    }
}
