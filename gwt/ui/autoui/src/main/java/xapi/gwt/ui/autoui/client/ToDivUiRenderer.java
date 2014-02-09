package xapi.gwt.ui.autoui.client;

import java.io.IOException;

import xapi.ui.autoui.api.HasNamedValue;
import xapi.ui.autoui.api.UiRenderer;
import xapi.ui.autoui.api.UiRendererSelector;
import xapi.ui.autoui.api.UiRenderingContext;
import xapi.ui.autoui.api.UserInterface;
import xapi.util.X_Debug;

public class ToDivUiRenderer<T> implements UiRenderer<T>, UiRendererSelector {

  @Override
  public UiRenderer<T> renderInto(UserInterface<?> ui, UiRenderingContext ctx, HasNamedValue<T> data) {
    if (ui instanceof Appendable) {
      Object value = data.getValue();
      try {
        Appendable out = (Appendable)ui;
        if (ctx.isTemplateSet()) {
          String name = data.getName();
          out.append(ctx.getTemplate().apply(name, value));
        } else {
          out.append(String.valueOf(value));
        }
      } catch (IOException e) {
        throw X_Debug.rethrow(e);
      }
    }
    return this;
  }

  @Override
  public boolean useRenderer(UserInterface<?> ui, UiRenderer<?> renderer, Object o) {
    return true;
  }

}
