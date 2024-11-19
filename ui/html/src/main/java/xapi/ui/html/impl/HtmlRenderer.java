package xapi.ui.html.impl;

import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.fu.In1Out1;
import xapi.fu.Lazy;
import xapi.fu.Out1;
import xapi.ui.api.StyleService;
import xapi.ui.html.X_Html;
import xapi.ui.html.api.HtmlSnippet;
import xapi.util.api.ConvertsValue;

public class HtmlRenderer {

  @SuppressWarnings("unchecked")
  private StringTo<In1Out1<?, String>> map = X_Collect.newStringMap(
      Class.class.cast(ConvertsValue.class)
  );

  @SuppressWarnings("unchecked")
  public <T> In1Out1<T, String> getRenderer(Class<T> type, StyleService<?, ?> context) {
    In1Out1<T, String> converter = (In1Out1<T, String>) map.get(type.getName());
    if (converter == null) {
      converter = buildConverter(type, context);
      map.put(type.getName(), converter);
    }
    return converter;
  }

  protected <T> In1Out1<T, String> buildConverter(final Class<T> type, final StyleService<? ,?> context) {
    final Out1<HtmlSnippet<T>> snippet = Lazy.deferred1(X_Html::toSnippet, type, context);
    return from -> snippet.out1().convert(from);
  }

}
