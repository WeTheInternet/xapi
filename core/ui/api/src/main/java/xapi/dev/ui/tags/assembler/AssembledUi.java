package xapi.dev.ui.tags.assembler;

import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.ui.api.ComponentBuffer;
import xapi.dev.ui.api.GeneratedUiComponent;
import xapi.dev.ui.api.UiNamespace;
import xapi.dev.ui.impl.UiGeneratorTools;

/**
 * A container for information about a ui that is being assembled by a given component.
 *
 * This contains information about shadow and light DOM, names (and, later, types) of referenced elements.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 7/24/18.
 */
public class AssembledUi {

    private final UiGeneratorTools tools;
    private final UiNamespace namespace;
    private final ComponentBuffer source;

    public AssembledUi(ComponentBuffer ui, UiGeneratorTools tools, UiNamespace namespace) {
        this.source = ui;
        this.tools = tools;
        this.namespace = namespace;
    }

    public GeneratedUiComponent getUi() {
        return source.getGeneratedComponent();
    }

    public UiGeneratorTools getTools() {
        return tools;
    }

    public ApiGeneratorContext getContext() {
        return source.getContext();
    }

    public UiNamespace getNamespace() {
        return namespace;
    }
}
