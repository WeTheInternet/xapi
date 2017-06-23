package xapi.server.gen;

import xapi.dev.ui.api.UiImplementationGenerator;
import xapi.fu.MappedIterable;
import xapi.fu.iterate.SingletonIterator;

import java.util.Collections;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/23/16.
 */
public class VertxWebAppGenerator extends WebAppGenerator {

    @Override
    protected MappedIterable<UiImplementationGenerator> getImplementations() {
        return SingletonIterator.singleItem(new VertxWebAppImplGenerator());
    }
}
