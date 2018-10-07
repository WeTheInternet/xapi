package xapi.source.api;

/**
 * Represents an arbitrary class argument, most suitable as the component type of a type argument:
 *
 * Given:
 * {@code List<Bag<?>[]> }
 * List is a IsClassParameter,
 * Bag is an IsClassArgument, which is the bounds of the List IsClassParameter
 * ? is a WildcardArgument
 *
 * An IsClassParameter cannot directly contain wildcards or arrays,
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 10/7/18 @ 2:38 AM.
 */
public interface IsClassArgument extends IsTypeArgument, HasBounds {

    int getArrayDepth();

}
