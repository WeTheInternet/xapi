package xapi.dev.opts;

import xapi.args.ArgHandlerFlag;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/12/17.
 */
public class ArgHandlerServerless extends ArgHandlerFlag {

    @Override
    public String getPurpose() {
        return "Run application without any server";
    }

    @Override
    public String getTag() {
        return "serverless";
    }
}
