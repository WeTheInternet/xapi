package xapi.ui.html.api;

import xapi.ui.api.StyleService;


public interface HtmlService {

  <T> HtmlSnippet<T> toSnippet(Class<?> templateClass, Class<? extends T> cls, StyleService<?, ?> buffer);

  void injectStyle(Class<?> cls, StyleService<?, ?> context);

}
