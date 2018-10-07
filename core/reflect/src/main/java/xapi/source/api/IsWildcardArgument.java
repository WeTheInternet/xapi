package xapi.source.api;

import xapi.source.impl.ImmutableType;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 10/7/18 @ 1:02 AM.
 */
public interface IsWildcardArgument extends IsTypeArgument, HasBounds {

    IsType WILDCARD_TYPE = new ImmutableType("", "?");

    @Override
    default IsType getEnclosingType() {
        return null;
    }

    @Override
    default String getPackage() {
        return "";
    }

    @Override
    default String getEnclosedName() {
        return "?";
    }

    @Override
    default String getName() {
        return "?";
    }

    boolean isSuperArgument();
}
