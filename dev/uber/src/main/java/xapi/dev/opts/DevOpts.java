package xapi.dev.opts;

import xapi.args.KwArgProcessorBase;
import xapi.dev.opts.ArgHandlerHeadless;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/11/17.
 */
public class DevOpts extends KwArgProcessorBase {

    private final ArgHandlerHeadless headless = new ArgHandlerHeadless();
    private final ArgHandlerServerless serverless = new ArgHandlerServerless();

    public DevOpts() {
        registerHandler(headless);
        registerHandler(serverless);
    }

    public boolean isHeadless() {
        return headless.isSet();
    }

    public boolean isServerless() {
        return serverless.isSet();
    }
}
