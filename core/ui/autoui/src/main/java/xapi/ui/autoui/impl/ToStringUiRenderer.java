package xapi.ui.autoui.impl;

import java.io.IOException;

import xapi.ui.autoui.api.HasNamedValue;
import xapi.ui.autoui.api.UiRenderer;
import xapi.ui.autoui.api.UiRenderingContext;
import xapi.ui.autoui.api.UserInterface;
import xapi.util.X_Debug;

@SuppressWarnings("rawtypes")
public class ToStringUiRenderer implements UiRenderer{

  @Override
  public UiRenderer renderInto(UserInterface ui, UiRenderingContext ctx, HasNamedValue data) {
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

}
