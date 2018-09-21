package xapi.collect.impl;

import xapi.annotation.gc.NotReusable;
import xapi.fu.itr.MappedIterable;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 2/6/16.
 */
public interface NonReusableIterable <T> extends MappedIterable<T>, NotReusable {

}
