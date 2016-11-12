package xapi.fu.has;

import xapi.fu.MappedIterable;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/8/16.
 */
public interface HasItems <T> {

    MappedIterable<T> forEachItem();

}
