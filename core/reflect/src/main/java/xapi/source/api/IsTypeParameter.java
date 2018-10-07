package xapi.source.api;

import xapi.fu.itr.SizedIterable;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 10/6/18 @ 11:25 PM.
 */
public interface IsTypeParameter extends HasAnnotations, HasBounds {

    String getName();

    @Override
    SizedIterable<IsParameterizedType> getBounds();

}
