/**
 *
 */
package xapi.ui.api;

import xapi.fu.Out1;
import xapi.ui.api.style.HasStyleResources;

/**
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
@SuppressWarnings("rawtypes")
public interface StyleService<
    /**
     * The type of your raw style element.
     * TODO: make a shared abstraction and erase this parameter
    */
    Style,
    /**
     * The bundle type you want.
     */
    Bundle extends HasStyleResources> {

  static String toGoogleFontUrl(String... fonts) {
    StringBuilder b = new StringBuilder(
        "@import url("
            + "https://fonts.googleapis.com/css?family=");
    for (int i = 0; i < fonts.length; i++) {
      String font = fonts[i];
      if (i > 0) {
        b.append("|");
      }
      // TODO proper uri encoding later
      b.append(font.replace(' ', '+'));
    }
    b.append(");");
    return b.toString();
  }

  void addCss(String css, int priority);

  void flushCss();

  void loadGoogleFonts(String ... fonts);

  Style injectStyle(Class<? extends Bundle> bundle, Class<?> ... styles);

  Out1<Style> registerStyle(Class<? extends Bundle> bundle, String css, Class<?> ... styles);

}
