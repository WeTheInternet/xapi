package xapi.dev.components;

import xapi.dev.ui.api.GeneratedUiComponent;
import xapi.dev.ui.api.GeneratedUiImplementation;
import xapi.dev.ui.api.UiNamespace;
import xapi.fu.Lazy;
import xapi.inject.X_Inject;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/8/17.
 */
public class GeneratedWebComponent extends GeneratedUiImplementation {

    private final Lazy<UiNamespaceGwt> namespace;

    public GeneratedWebComponent(GeneratedUiComponent ui) {
        super(ui, ui.getPackageName());
        namespace = Lazy.deferred1(()-> X_Inject.instance(UiNamespaceGwt.class));
    }

    @Override
    public String getAttrKey() {
        return "gwt";
    }

    @Override
    public UiNamespace reduceNamespace(UiNamespace from) {
        return namespace.out1();
    }
}
