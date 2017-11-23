package xapi.dist.api;

import xapi.args.ArgHandlerFile;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/12/17.
 */
public class ArgHandlerOutputDir extends ArgHandlerFile {

    @Override
    public String getPurpose() {
        return "The directory to put exported generated content into";
    }

    @Override
    public String getTag() {
        return "output";
    }
}
