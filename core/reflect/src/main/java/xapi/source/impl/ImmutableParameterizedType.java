package xapi.source.impl;

import xapi.fu.Maybe;
import xapi.fu.X_Fu;
import xapi.fu.itr.ArrayIterable;
import xapi.fu.itr.SizedIterable;
import xapi.source.api.*;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 10/7/18 @ 2:57 AM.
 */
public class ImmutableParameterizedType extends ImmutableTypeArgument implements IsParameterizedType {

    private final IsTypeArgument[] bounds;

    public ImmutableParameterizedType(IsTypeParameter parameter, IsType type, IsTypeArgument ... bounds) {
        super(parameter, type);
        this.bounds = bounds;
    }

//    @Override
//    public IsClass getType() {
//        return (IsClass) super.getType();
//    }

    @Override
    public String getName() {
        return getType().getEnclosedName();
    }

    @Override
    public String toSource() {
        StringBuilder b = new StringBuilder(super.toSource());
        if (X_Fu.isEmpty(bounds)) {
            return b.toString();
        }
        b.append("<");
        String prefix = "";
        for (IsTypeArgument bound : bounds) {
            b.append(prefix).append(bound.toSource());
            prefix = ", ";
        }
        b.append(">");
        return b.toString();
    }

    @Override
    public SizedIterable<? extends IsTypeArgument> getBounds() {
        return ArrayIterable.iterate(bounds);
    }
}
