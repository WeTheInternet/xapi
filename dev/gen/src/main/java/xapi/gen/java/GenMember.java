package xapi.gen.java;

import xapi.gen.GenBuffer;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 12/12/15.
 */
public interface GenMember<P extends GenBuffer<?, P>, S extends GenMember<P, S>> extends GenBuffer<P, S> {
}
