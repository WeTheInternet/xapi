package xapi.source.impl;

import xapi.fu.X_Fu;
import xapi.fu.itr.SizedIterable;
import xapi.source.api.IsClassArgument;
import xapi.source.api.IsType;
import xapi.source.api.IsTypeArgument;
import xapi.source.api.IsTypeParameter;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 10/7/18 @ 3:02 AM.
 */
public class ImmutableClassArgument extends ImmutableTypeArgument implements IsClassArgument {

    private final int arrayDepth;
    private final IsTypeArgument[] bounds;

    public ImmutableClassArgument(IsTypeParameter parameter, IsType type, int arrayDepth, IsTypeArgument ... bounds) {
        super(parameter, type);
        this.arrayDepth = arrayDepth;
        this.bounds = bounds;
    }

    @Override
    public String toSource() {
        StringBuilder b = new StringBuilder(super.toSource());
        if (X_Fu.isNotEmpty(bounds)) {
            b.append("<");
            String suffix = "";
            for (IsTypeArgument bound : bounds) {
                b.append(bound.toSource()).append(suffix);
                suffix = ", ";
            }
            b.append(">");
        }
        return b.toString();
    }

    @Override
    public int getArrayDepth() {
        return arrayDepth;
    }

    @Override
    public SizedIterable<? extends IsTypeArgument> getBounds() {
        return getBounds();
    }
}
