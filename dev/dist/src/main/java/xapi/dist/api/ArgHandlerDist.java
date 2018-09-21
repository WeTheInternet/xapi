package xapi.dist.api;

import xapi.args.ArgHandlerString;
import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.fu.itr.SizedIterable;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/12/17.
 */
public class ArgHandlerDist extends ArgHandlerString {

    private final IntTo<String> dist = X_Collect.newList(String.class);

    @Override
    public boolean setString(String str) {
        dist.add(str);
        return true;
    }

    @Override
    public String getPurpose() {
        return "Specify fully qualified classnames to treat as dist-build entry points";
    }

    @Override
    public boolean isMultiUse() {
        return true;
    }

    @Override
    public String getTag() {
        return "entry";
    }

    @Override
    public String[] getTagArgs() {
        return new String[]{ "com.fu.MyEntryPoint" };
    }

    public SizedIterable<String> getDistEntryPoints() {
        return dist;
    }
}
