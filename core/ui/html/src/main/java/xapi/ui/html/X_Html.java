package xapi.ui.html;

import xapi.annotation.compile.MagicMethod;
import xapi.inject.X_Inject;
import xapi.ui.api.StyleService;
import xapi.ui.html.api.HtmlService;
import xapi.ui.html.api.HtmlSnippet;

public class X_Html {

  private X_Html() {}

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @MagicMethod(doNotVisit = true)
  public static <T> String toHtml(Class<?> template, Class<? extends T> cls, T o, StyleService<?, ?> context) {
    return toSnippet(template, (Class)cls, context).convert(o);
  }

  @MagicMethod(doNotVisit = true)
  public static <T> String toHtml(Class<? extends T> cls, T o, StyleService<?, ?> context) {
    return toHtml(cls,  cls, o, context);
  }

  @MagicMethod(doNotVisit = true)
  public static <T> HtmlSnippet<T> toSnippet(Class<? extends T> cls, StyleService<?, ?> context) {
    return toSnippet(cls, cls, context);
  }

  @MagicMethod(doNotVisit = true)
  public static <T> HtmlSnippet<T> toSnippet(Class<?> templateClass, Class<? extends T> cls, StyleService<?, ?> context) {
    return X_Inject.singleton(HtmlService.class).toSnippet(templateClass, cls, context);
  }

  @MagicMethod(doNotVisit = true)
  public static void injectCss(final Class<?> cls, StyleService<?, ?> context) {
    X_Inject.singleton(HtmlService.class).injectStyle(cls, context);
  }

}
