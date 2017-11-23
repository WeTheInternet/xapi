package xapi.dev.opts;

import xapi.args.ArgHandlerFlag;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/12/17.
 */
public class ArgHandlerHeadless extends ArgHandlerFlag {

    @Override
    public String getPurpose() {
        return "Run application without any GUI";
    }

    @Override
    public String getTag() {
        return "headless";
    }
}
