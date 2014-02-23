package xapi.ui.autoui.api;

public interface UiRendererSelector {

  final UiRendererSelector ALWAYS_TRUE = new AlwaysTrue();
  
  boolean useRenderer(UserInterface<?> ui, UiRenderer<?> renderer, String path, Object o);
  
}
