package xapi.source.api;

/**
 * Represents a reified type in a field, method or type implementation.
 *
 * <pre>
 * Given:
 * class X<Y> extends Z<Y> { }
 * In X<Y>, Y is a type parameter of the IsTypeDeclaration of IsClassDeclaration X.
 * In Z<Y>, Y is a type argument of the IsTypeArgument of superclass field of IsClassDeclaration X.
 * And:
 * X<A> variable = new X<>();
 * A is a type argument of an IsTypeArgument[] field in IsClassParameter X
 * And:
 * List<X> list = ...
 * X is a type argument of an IsTypeArgument[] field in IsClassParameter List
 * And:
 * List<? extends X> list = ...
 * X is a type argument of an IsTypeArgument[] field in an IsWildcardArgument,
 * which is a type argument of an IsTypeArgument[] field in IsClassParameter List
 * </pre>
 *
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 10/7/18 @ 12:52 AM.
 */
public interface IsTypeArgument extends IsNamedType {

    IsTypeArgument[] EMPTY_ARRAY = {};
    /**
     * This is here so you can safely send null to a varargs method which
     * null-checks and uses EMPTY_ARRAY.  This saves the array creation of varargs,
     * and avoids accidentally sending {null} (array of one item of null), and/or ugly (casts)null.
     */
    IsTypeArgument[] NULL_ARRAY = null;

    /**
     * The type parameter this type argument is fulfilling.
     *
     * May come from a foreign class, like a type argument of a List variable:
     * {@code List<? extends Sometype> var = ...}
     * The wildcard argument would have List's IsClassDeclaration's type parameter (E) as it's parameter.
     * Sometype is an IsClassParameter, which has no notion of binding to a type parameter.
     */
    IsTypeParameter getParameter();

    IsType getType(); // hm... maybe take this away? or make it IsTypeDeclaration?

    default IsType getRawType() {
        return getType().getRawType();
    }

    default IsTypeArgument getArrayType() {
        throw new UnsupportedOperationException();
    }

    @Override
    default String getName() {
        return getParameter().getName();
    }
}
