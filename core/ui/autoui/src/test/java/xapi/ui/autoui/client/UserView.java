package xapi.ui.autoui.client;

import xapi.ui.autoui.api.UiRendererOptions;
import xapi.ui.autoui.impl.ToStringUiRenderer;

@UiRendererOptions(
  renderers = ToStringUiRenderer.class,
  isWrapper = true,
  template = "$name: $value,\n"
)
public interface UserView extends User {
}
