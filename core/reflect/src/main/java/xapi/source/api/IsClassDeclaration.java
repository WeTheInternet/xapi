package xapi.source.api;

import xapi.fu.Maybe;

/**
 * Represents the declaration of a class, interface, enum or annotation,
 * and it's type parameters (if applicable).
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 10/7/18 @ 1:18 AM.
 */
public interface IsClassDeclaration extends IsTypeDeclaration, HasQualifiedName {

    IsParameterizedType getSuperType();

    @Override
    default Maybe<IsParameterizedType> getExtends() {
        return Maybe.deferred(this::getSuperType);
    }
}
