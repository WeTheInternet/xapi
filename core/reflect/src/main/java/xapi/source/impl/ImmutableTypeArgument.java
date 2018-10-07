package xapi.source.impl;

import xapi.source.api.IsType;
import xapi.source.api.IsTypeArgument;
import xapi.source.api.IsTypeParameter;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 10/7/18 @ 1:52 AM.
 */
public class ImmutableTypeArgument implements IsTypeArgument {
    // perhaps make this a supplier of a type parameter?
    private final IsTypeParameter parameter;
    private final IsType type; // consider making this IsTypeDeclaration

    public ImmutableTypeArgument(IsTypeParameter parameter, IsType type) {
        this.parameter = parameter;
        this.type = type;
    }

    @Override
    public IsTypeParameter getParameter() {
        return parameter;
    }

    @Override
    public IsType getType() {
        return type;
    }

    @Override
    public IsType getEnclosingType() {
        return type.getEnclosingType();
    }

    @Override
    public String getPackage() {
        return type.getPackage();
    }

    @Override
    public String getEnclosedName() {
        return type.getEnclosedName();
    }
}
