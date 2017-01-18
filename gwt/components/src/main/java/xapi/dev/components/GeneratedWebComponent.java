package xapi.dev.components;

import xapi.dev.ui.GeneratedUiComponent;
import xapi.dev.ui.GeneratedUiComponent.GeneratedUiImplementation;
import xapi.dev.ui.UiNamespace;
import xapi.fu.Lazy;
import xapi.inject.X_Inject;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/8/17.
 */
public class GeneratedWebComponent extends GeneratedUiImplementation {

    private final Lazy<UiNamespaceGwt> namespace;

    public GeneratedWebComponent(GeneratedUiComponent ui) {
        super(ui.getPackageName(), ui.getApi(), ui.getBase());
        namespace = Lazy.deferred1(()-> X_Inject.instance(UiNamespaceGwt.class));
    }

    @Override
    public UiNamespace reduceNamespace(UiNamespace from) {
        return namespace.out1();
    }
}
