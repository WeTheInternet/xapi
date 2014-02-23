package xapi.ui.autoui.api;

public final class AlwaysTrue implements UiRendererSelector {
  @Override
  public boolean useRenderer(UserInterface<?> ui, UiRenderer<?> renderer, String path, Object o) {
    return true;
  }
}