package xapi.server.gen;

import xapi.dev.ui.AbstractUiImplementationGenerator;
import xapi.dev.ui.GeneratedUiComponent;
import xapi.dev.ui.GeneratedUiComponent.GeneratedUiImplementation;
import xapi.server.vertx.api.VertxPlatform;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/23/16.
 */
public class VertxWebAppImplGenerator extends AbstractUiImplementationGenerator<WebAppGeneratorContext>{

    @Override
    protected String getImplPrefix() {
        return "Vertx";
    }

    @Override
    protected void initializeComponent(GeneratedUiComponent result) {
        if (result.addImplementationFactory(VertxPlatform.class, GeneratedVertxComponent::new)) {
            super.initializeComponent(result);
        }
    }

    @Override
    public GeneratedUiImplementation getImpl(GeneratedUiComponent component) {
        for (GeneratedUiImplementation impl : component.getImpls()) {
            if (impl instanceof GeneratedVertxComponent) {
                return impl;
            }
        }
        throw new IllegalStateException("No vertx impl found in " + component.getImpls());
    }
}
