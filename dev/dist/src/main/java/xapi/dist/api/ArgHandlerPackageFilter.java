package xapi.dist.api;

import xapi.args.ArgHandlerString;
import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.fu.itr.SizedIterable;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/12/17.
 */
public class ArgHandlerPackageFilter extends ArgHandlerString {

    private final IntTo<String> filters = X_Collect.newList(String.class);

    @Override
    public boolean setString(String str) {
        filters.add(str);
        return true;
    }

    @Override
    public String getPurpose() {
        return "Specify filters to use to narrow package searching; defaults to searching all packages";
    }

    @Override
    public boolean isMultiUse() {
        return true;
    }

    @Override
    public String getTag() {
        return "package";
    }

    @Override
    public String[] getTagArgs() {
        return new String[]{ "com.fu" };
    }

    public SizedIterable<String> getPackageFilters() {
        return filters;
    }
}
