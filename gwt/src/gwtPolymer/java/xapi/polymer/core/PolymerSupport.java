/**
 *
 */
package xapi.polymer.core;

/**
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public class PolymerSupport {

  public static native PolymerElement setPolymerAttr(PolymerElement group,
      String string)
  /*-{
    group.setAttribute(string, "");
    return group;
  }-*/;

}
