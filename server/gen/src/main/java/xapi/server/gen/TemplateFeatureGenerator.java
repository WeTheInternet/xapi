package xapi.server.gen;

import xapi.dev.ui.api.UiFeatureGenerator;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/9/16.
 */
public class TemplateFeatureGenerator extends UiFeatureGenerator {

    private final WebAppComponentGenerator owner;

    public TemplateFeatureGenerator(WebAppComponentGenerator owner) {
        this.owner = owner;
    }
}
