package xapi.gwt.ui.autoui.client;

import xapi.gwt.ui.autoui.api.IsSafeHtmlBuilder;
import xapi.ui.autoui.api.UiRenderer;
import xapi.ui.autoui.api.UiRendererSelector;
import xapi.ui.autoui.api.UiRenderingContext;
import xapi.ui.autoui.api.UserInterface;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

public class ToHtmlUiRenderer<T> implements UiRenderer<T>, UiRendererSelector {

  @Override
  public UiRenderer<T> renderInto(UserInterface<?> ui, UiRenderingContext ctx, String path, T data) {
    assert (ui instanceof IsSafeHtmlBuilder);
    SafeHtmlBuilder out = ((IsSafeHtmlBuilder)ui).getSafeHtmlBuilder();
    if (ctx.isTemplateSet()) {
      String html = ctx.applyTemplate(path, data);
      out.appendHtmlConstant(html);
    } else {
      if (data instanceof SafeHtml) {
        out.appendHtmlConstant(((SafeHtml)data).asString());
      } else {
        out.appendHtmlConstant(String.valueOf(data));
      }
    }
    return this;
  }

  @Override
  public boolean useRenderer(UserInterface<?> ui, UiRenderer<?> renderer, String path, Object o) {
    return ui instanceof IsSafeHtmlBuilder;
  }

}
