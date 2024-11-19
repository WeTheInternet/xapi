package xapi.ui.autoui.api;

public interface UiRendererSelector {

  UiRendererSelector ALWAYS_TRUE = new AlwaysTrue();

  boolean useRenderer(UserInterface<?> ui, UiRenderer<?> renderer, String path, Object o);

}
