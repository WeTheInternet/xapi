package xapi.ui.html.api;

import xapi.dev.source.HtmlBuffer;

public interface HtmlService {

  <T> HtmlSnippet<T> toSnippet(Class<? extends T> cls, HtmlBuffer buffer);
  
}
