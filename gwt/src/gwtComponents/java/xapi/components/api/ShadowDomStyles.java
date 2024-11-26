package xapi.components.api;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 2/8/16.
 */ // used so we can make ShadowDomStyle repeatable,
// we don't actually use this in our api, as it is just an extra wrapper.
public @interface ShadowDomStyles {
  ShadowDomStyle[] value();
}
