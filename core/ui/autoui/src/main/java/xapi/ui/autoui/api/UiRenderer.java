package xapi.ui.autoui.api;

public interface UiRenderer <T> {

  UiRenderer<T> renderInto(UserInterface<?> ui, UiRenderingContext ctx, HasNamedValue<T> data);

}
