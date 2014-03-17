package xapi.gwt.ui.autoui.client;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

import xapi.gwt.ui.autoui.api.IsSafeHtmlBuilder;
import xapi.ui.autoui.api.UiRenderingContext;
import xapi.ui.autoui.impl.AbstractUserInterface;

public class SafeHtmlUserInterface <T> extends AbstractUserInterface<T> implements IsSafeHtmlBuilder {

  private final SafeHtmlBuilder out = new SafeHtmlBuilder();
  
  @Override
  protected void doRender(UiRenderingContext ctx, T model) {
    startRender(out, model);
    recursiveRender(ctx, ctx.getRenderer(), model);
    endRender(out, model);
  }

  protected void endRender(SafeHtmlBuilder out, T model) {
    
  }

  protected void startRender(SafeHtmlBuilder out, T model) {
    
  }
  
  public SafeHtml getSafeHtml() {
    return out.toSafeHtml();
  }

  @Override
  public SafeHtmlBuilder getSafeHtmlBuilder() {
    return out;
  }

}
