package xapi.fu.api;

/**
 * An interface for objects that can duplicate themselves.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 8/19/17.
 */
public interface Copyable <T> {

    T copy();

}
