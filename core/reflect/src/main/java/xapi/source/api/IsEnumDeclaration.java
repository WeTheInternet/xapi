package xapi.source.api;

import xapi.fu.itr.SizedIterable;
import xapi.source.util.X_Modifier;
import xapi.source.impl.*;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 10/7/18 @ 2:54 AM.
 */
public interface IsEnumDeclaration extends IsTypeDeclaration, HasSupertype {

    SizedIterable<IsEnumArgument> getArguments();

    @Override
    default IsParameterizedType getSupertype() {
        final AbstractClass enumType = new AbstractClass(null, "java.lang", "Enum", X_Modifier.PUBLIC);
        final IsType selfRef = new ImmutableType("", "E");
        final IsTypeArgument ref = new ImmutableTypeArgument(null, selfRef);
        final IsParameterizedType extendsSelf = new ImmutableParameterizedType(null, enumType, ref);
        final IsTypeParameter param = new ImmutableTypeParameter("E", extendsSelf);
        enumType.addTypeParameters(param);
        final IsParameterizedType ENUM_PARAM = new ImmutableParameterizedType(param, enumType);
        return ENUM_PARAM;
    }
}
