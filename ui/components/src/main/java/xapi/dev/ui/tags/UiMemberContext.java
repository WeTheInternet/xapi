package xapi.dev.ui.tags;

import xapi.dev.lang.gen.ApiGeneratorContext;
import xapi.dev.ui.impl.UiGeneratorTools;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/28/18.
 */
class UiMemberContext extends ApiGeneratorContext<UiMemberContext> {

    private final UiGeneratorTools tools;
    private final ApiGeneratorContext ctx;

    public UiMemberContext(UiGeneratorTools tools, ApiGeneratorContext ctx) {
        super(ctx);
        this.tools = tools;
        this.ctx = ctx;
    }

    public UiGeneratorTools getTools() {
        return tools;
    }

    public ApiGeneratorContext getApiContext() {
        return ctx;
    }
}
