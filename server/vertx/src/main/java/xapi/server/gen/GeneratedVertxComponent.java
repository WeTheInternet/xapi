package xapi.server.gen;

import xapi.dev.api.GeneratedTypeOwner;
import xapi.dev.ui.api.GeneratedUiImplementation;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/15/17.
 */
public class GeneratedVertxComponent extends GeneratedUiImplementation {

    public GeneratedVertxComponent(GeneratedTypeOwner ui) {
        super(ui, ui.getPackageName());
    }

    @Override
    public String getAttrKey() {
        return "vertx";
    }
}
