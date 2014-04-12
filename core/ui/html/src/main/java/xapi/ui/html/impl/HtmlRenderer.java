package xapi.ui.html.impl;

import javax.inject.Provider;

import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.dev.source.HtmlBuffer;
import xapi.ui.html.X_Html;
import xapi.ui.html.api.Html;
import xapi.ui.html.api.HtmlSnippet;
import xapi.util.api.ConvertsValue;
import xapi.util.impl.LazyProvider;

public class HtmlRenderer {

  @SuppressWarnings("unchecked")
  private StringTo<ConvertsValue<?, String>> map = X_Collect.newStringMap(
      Class.class.cast(ConvertsValue.class)
  );
  
  public <T> ConvertsValue<T, String> getRenderer(Class<T> type, HtmlBuffer context) {
    ConvertsValue<T, String> converter = (ConvertsValue<T, String>) map.get(type.getName());
    if (converter == null) { 
      converter = buildConverter(type, context);
      map.put(type.getName(), converter);
    }
    return converter;
  }
  
  protected <T> ConvertsValue<T, String> buildConverter(final Class<T> type, final HtmlBuffer context) {
    final Provider<HtmlSnippet<T>> snippet = 
      new LazyProvider<>(new Provider<HtmlSnippet<T>>() {
        @Override
        public HtmlSnippet<T> get() {
          return X_Html.toSnippet(type, context);
        }
      });
    return new ConvertsValue<T, String>(){
      @Override
      public String convert(T from) {
        return snippet.get().convert(from);
      }
    };
  }
  
}
