package xapi.annotation.compile;

/**
 * Use this in case you want to add support for implementing some interface,
 * but you can't guarantee it is on the classpath (an optional dependency,
 * or, heaven forbid, a cyclic dependency).  This can also be handy
 * if you have a predictable generated classname of an interface to implement.
 *
 * You may specify the interface to implement by class or by string,
 * and if the referenced type is not available at final compile time,
 * then the interface will be converted to string form.
 *
 * TODO: finish and release javac plugin to actually do this.
 *
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 1/9/16.
 */
public @interface ImplementIfPresent {
  Reference value();
}
