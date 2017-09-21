package xapi.server.gen;

import xapi.dev.ui.api.UiImplementationGenerator;
import xapi.fu.In1Out1;
import xapi.fu.MappedIterable;
import xapi.fu.iterate.SingletonIterator;
import xapi.javac.dev.model.CompilerSettings;

import java.util.Collections;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/23/16.
 */
public class VertxWebAppGenerator extends WebAppGenerator {

    public VertxWebAppGenerator() {
        super();
    }
    public VertxWebAppGenerator(In1Out1<CompilerSettings, CompilerSettings> settings) {
        super(settings);
    }

    @Override
    protected MappedIterable<UiImplementationGenerator> getImplementations() {
        return SingletonIterator.singleItem(new VertxWebAppImplGenerator(this));
    }
}
