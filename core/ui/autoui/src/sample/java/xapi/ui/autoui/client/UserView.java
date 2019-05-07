package xapi.ui.autoui.client;

import xapi.ui.autoui.api.UiOptions;
import xapi.ui.autoui.api.UiRendererOptions;
import xapi.ui.autoui.impl.ToStringUiRenderer;

@UiRendererOptions(
  renderers = ToStringUiRenderer.class,
  isWrapper = true,
  template = "$name: $value,\n"
)
@UiOptions(
  fields = {"email","name","id"}
)
public interface UserView extends User {
  @Override
  public String id();
}
