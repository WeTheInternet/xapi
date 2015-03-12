/**
 *
 */
package xapi.ui.api;

/**
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
@SuppressWarnings("rawtypes")
public interface StyleService<T extends StyleService> {

  T addCss(String css, int priority);

  void flushCss();

}
