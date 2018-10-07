package xapi.source.api;

import xapi.fu.itr.SizedIterable;

/**
 * Represents a bounded type argument.
 *
 * Example:
 * {@code List<? extends Sometype & Othertype> }
 *
 * {@link IsWildcardArgument} implements HasBounds;
 * Sometype and Othertype are {@link IsParameterizedType} bounds of a wildcard.
 *
 * That wildcard itself is the bounds of the List IsClassParameter.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 10/7/18 @ 2:12 AM.
 */
public interface HasBounds {

    SizedIterable<? extends IsTypeArgument> getBounds();

}
