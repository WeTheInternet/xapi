/**
 *
 */
package xapi.ui.style;

import xapi.collect.api.StringTo;
import xapi.fu.Out1;
import xapi.inject.X_Inject;
import xapi.platform.GwtPlatform;
import xapi.ui.api.StyleCacheService;
import xapi.ui.api.StyleService;
import xapi.ui.html.api.GwtStyles;

import javax.inject.Provider;
import java.util.Arrays;

import static xapi.collect.X_Collect.newStringMap;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

/**
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
@SuppressWarnings("rawtypes")
@GwtPlatform
public abstract class StyleServiceAbstract <StyleElement> implements StyleService<StyleElement, GwtStyles> {

  private final Provider<StyleCacheService<StyleElement, GwtStyles>> styleCache = X_Inject.singletonLazy(StyleCacheService.class);


  protected ScheduledCommand printPendingCss;
  protected final StringTo<String> pendingCss = newStringMap(String.class);
  protected final StringTo<StyleElement> liveCss = liveCssMap();

  protected abstract StringTo<StyleElement> liveCssMap();

  @Override
  @SuppressWarnings("unchecked")
  public void addCss(final String css, final int priority) {
    if (printPendingCss == null) {
      css(css);
    }
    final String key = Integer.toString(priority);
    final String was = pendingCss.get(key);
    if (was == null) {
      pendingCss.put(key, css);
    } else {
      pendingCss.put(key, was + "\n" + css);
    }
  }

  protected void css(final String css) {
    printPendingCss = this::flushCss;
    Scheduler.get().scheduleFinally(printPendingCss);
  }

  @Override
  public void flushCss() {
    printPendingCss = null;
    final StringTo<String> pending = pendingCss;
    synchronized (pendingCss) {
      final String[] keys = pending.keyArray();
      Arrays.sort(keys);
      for (final String key : keys) {
        final String css = pending.get(key);
        final StyleElement style = getStyleElement(key);
        appendStyle(style, css);
      }
    }
    pendingCss.clear();
  }

  private native void appendStyle(StyleElement style, String css)
  /*-{
		if (style.styleSheet) {// IE
			style.styleSheet.cssText = style.styleSheet.cssText + css;
		} else {
			style.appendChild($doc.createTextNode(css));
		}
  }-*/;

  private StyleElement getStyleElement(final String priority) {
    StyleElement style = liveCss.get(priority);
    if (style == null) {
      try {
        style = prioritizedStyle(priority);
      } finally {
        liveCss.put(priority, style);
      }
    }
    return style;
  }

  protected abstract StyleElement prioritizedStyle(String priority);

  @Override
  public void loadGoogleFonts(String ... fonts) {
    addCss(StyleService.toGoogleFontUrl(fonts), 0);
  }

  @Override
  public StyleElement injectStyle(
      Class<? extends GwtStyles> bundle, Class<?> ... styles
  ) {
    return styleCache.get().injectStyle(bundle, styles);
  }

  @Override
  @SafeVarargs
  public final Out1<StyleElement> registerStyle(
      Class<? extends GwtStyles> bundle, String css, Class<?>... styles
  ) {
    styleCache.get().registerStyle(bundle, css, styles);
    return ()->injectStyle(bundle, styles);
  }

}
