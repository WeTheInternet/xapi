package xapi.ui.html;

import xapi.dev.source.HtmlBuffer;
import xapi.inject.X_Inject;
import xapi.ui.html.api.HtmlService;
import xapi.ui.html.api.HtmlSnippet;

public class X_Html {

  private X_Html() {}
   
  public static <T> String toHtml(Class<? extends T> cls, T o, HtmlBuffer context) {
    return toSnippet(o.getClass(), context).convert(o);
  }
  
  public static <T> HtmlSnippet<T> toSnippet(Class<? extends T> cls, HtmlBuffer context) {
    return X_Inject.singleton(HtmlService.class).toSnippet(cls, context);
  }
}
