package xapi.source.api;

import xapi.fu.Maybe;
import xapi.fu.data.MapLike;
import xapi.fu.itr.SizedIterable;

/**
 * Represents a type declaration; has a qualified name, type parameters,
 * and a list of implemented interfaces.
 *
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 10/7/18 @ 1:33 AM.
 */
public interface IsTypeDeclaration extends HasQualifiedName {

    MapLike<String, IsTypeParameter> getTypeParams();

    SizedIterable<IsParameterizedType> getImplements();

    default Maybe<IsParameterizedType> getExtends() {
        return Maybe.not();
    }

    default boolean isInterface() {
        return false;
    }

    default boolean isEnum() {
        return false;
    }
}

