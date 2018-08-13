package xapi.dev.ui.tags;

import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.ui.api.ContainerMetadata;
import xapi.dev.ui.impl.UiGeneratorTools;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/28/18.
 */
class UiMemberContext extends ApiGeneratorContext<UiMemberContext> {

    private UiTagGenerator generator;
    private ContainerMetadata container;
    private UiGeneratorTools tools;

    public UiMemberContext(ApiGeneratorContext ctx) {
        super(ctx);
    }

    public UiTagGenerator getGenerator() {
        return generator;
    }

    public UiMemberContext setGenerator(UiTagGenerator generator) {
        this.generator = generator;
        return this;
    }

    public ContainerMetadata getContainer() {
        return container;
    }

    public UiMemberContext setContainer(ContainerMetadata container) {
        this.container = container;
        return this;
    }

    public UiGeneratorTools getTools() {
        return tools;
    }

    public UiMemberContext setTools(UiGeneratorTools context) {
        this.tools = context;
        return this;
    }

}
