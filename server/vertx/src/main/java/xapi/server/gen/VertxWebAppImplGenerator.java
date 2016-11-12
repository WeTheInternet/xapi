package xapi.server.gen;

import xapi.dev.ui.AbstractUiImplementationGenerator;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/23/16.
 */
public class VertxWebAppImplGenerator extends AbstractUiImplementationGenerator<WebAppGeneratorContext>{

    @Override
    protected String getImplPrefix() {
        return "Vertx";
    }
}
