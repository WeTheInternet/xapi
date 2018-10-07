package xapi.source.api;

import xapi.fu.Maybe;

/**
 * Represents a reified class argument (field / method types, arguments to extends/implements).
 * <p>
 * The root type must be a real class type,
 * but your bounds can be any kind of type argument.
 * <p>
 * The use of this type requires a raw type of some kind; it can never be a wildcard or array type.
 * <p>
 * This type is only suitable for use as a component within a type parameter;
 * in particular, a root component of a type parameter; a class parameter has N
 * arbitrary type arguments (which may / likely include IsClassArgument instances).
 * <p>
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 10/7/18 @ 1:17 AM.
 */
public interface IsParameterizedType extends IsTypeArgument, HasBounds {
//
//    @Override
//    IsClass getType();
    // IsClass here is kinda gross, _should_ be IsGenericClass, but for now,
    // we're going to cheat and allow IsType, since we just want to limp this
    // along until WTI is released, and then come back around to make this ActuallyGood(tm)

    /**
     * TODO: track when a class argument is the bounds of a type argument,
     * as this might enable us to better resolve IsTypeParameter
     * (i.e. you could always find the source type parameter from every type argument).
     * <p>
     * This, however, would be sticky, since you may infer something about the type information
     * from a component type in a non-sensical way. i.e. checking parents would have
     * to be an opt-in of some kind, and simply not worth the effort at this time.
     */
    default Maybe<IsTypeArgument> getParent() {
        return Maybe.not();
    }

}
