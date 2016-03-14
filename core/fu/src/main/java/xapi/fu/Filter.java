package xapi.fu;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 2/14/16.
 */
public interface Filter<T> {

  boolean filter(T ... args);

}
