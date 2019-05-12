/**
 *
 */
package xapi.annotation.gwt;

/**
 * Used to mark types and methods as GwtIncompatible, so we don't have to
 * add gwt-user uberjar as a dependency in core classes that contain code
 * which is not compatible in Gwt.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public @interface GwtIncompatible {
}
