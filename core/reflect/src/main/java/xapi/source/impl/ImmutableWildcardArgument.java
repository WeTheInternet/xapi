package xapi.source.impl;

import xapi.fu.X_Fu;
import xapi.fu.itr.ArrayIterable;
import xapi.fu.itr.SizedIterable;
import xapi.source.api.IsTypeArgument;
import xapi.source.api.IsTypeParameter;
import xapi.source.api.IsWildcardArgument;

import java.util.List;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 10/7/18 @ 1:09 AM.
 */
public class ImmutableWildcardArgument extends ImmutableTypeArgument implements IsWildcardArgument {

    private final IsTypeArgument[] bounds;
    private final boolean superArgument;
    // Some ugly test code...
//abstract class S <X extends List<? super X>> implements List<List<? super X>[]> {
//    <Y extends List<? super X>> List<? super X> s(List<? extends X> s){
//        return (List<? super X>) s;
//    }
//}
    public ImmutableWildcardArgument(IsTypeParameter parameter, IsTypeArgument... bounds) {
        this(parameter, false, bounds);
    }

    public ImmutableWildcardArgument(IsTypeParameter parameter, boolean superArgument, IsTypeArgument... bounds) {
        super(parameter, IsWildcardArgument.WILDCARD_TYPE);
        this.superArgument = superArgument;
        this.bounds = bounds == null || bounds.length == 0 ? EMPTY_ARRAY : bounds;
    }

    @Override
    public SizedIterable<IsTypeArgument> getBounds() {
        return ArrayIterable.iterate(bounds);
    }

    @Override
    public boolean isSuperArgument() {
        return superArgument;
    }

    @Override
    public String toSource() {
        StringBuilder b = new StringBuilder(super.toSource());
        if (X_Fu.isEmpty(bounds)) {
            return b.toString();
        }
        String prefix = " extends ";
        for (IsTypeArgument bound : bounds) {
            b.append(prefix).append(bound.toSource());
            prefix = " & ";
        }
        return b.toString();
    }

}
