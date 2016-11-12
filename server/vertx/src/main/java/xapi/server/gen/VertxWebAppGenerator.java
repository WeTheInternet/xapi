package xapi.server.gen;

import xapi.dev.ui.UiImplementationGenerator;

import java.util.Collections;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/23/16.
 */
public class VertxWebAppGenerator extends WebAppGenerator {

    @Override
    protected Iterable<UiImplementationGenerator> getImplementations() {
        return Collections.singletonList(new VertxWebAppImplGenerator());
    }
}
