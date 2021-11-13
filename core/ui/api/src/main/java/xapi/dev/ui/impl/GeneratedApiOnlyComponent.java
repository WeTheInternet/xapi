package xapi.dev.ui.impl;

import xapi.dev.lang.gen.GeneratedTypeOwner;
import xapi.dev.ui.api.GeneratedUiImplementation;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/7/17.
 */
public class GeneratedApiOnlyComponent extends GeneratedUiImplementation {

    public GeneratedApiOnlyComponent(GeneratedTypeOwner owner) {
        super(owner, owner.getPackageName());
    }

    @Override
    public String getAttrKey() {
        return "api";
    }
}
