package xapi.ui.autoui.api;

public interface UiRenderer <T> {

  /**
   * Called when a renderer must render an object into a UserInterface.
   * <br/>
   * Calls to this method are guarded by {@link UiRendererSelector#useRenderer(UserInterface, UiRenderer, String, Object)}},
   * which can be used to filter incompatible UserInterfaces and UiRenderers.
   * <br/>
   * The {@link UiRendererSelector} used will be the one paired with the specified UiRenderer
   * inside of a {@link UiRendererOptions} annotation.
   * 
   * @param ui -> The UserInterface to render into.
   * @param ctx -> The {@link UiRenderingContext} containing the utilities needed to render the data
   * @param path -> The path of the data to render;
   * @param data -> The data to render
   * data can be retrieved from the data param using the {@link BeanValueProvider} found in the {@link UiRenderingContext#getBeanValueProvider()}.
   * @return -> this, for chaining, or a copy, for mutating.
   */
  UiRenderer<T> renderInto(UserInterface<?> ui, UiRenderingContext ctx, String path, T data);

}
