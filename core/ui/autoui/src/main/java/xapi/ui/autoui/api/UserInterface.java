package xapi.ui.autoui.api;

/**
 * This interface is used for classes who render a UI from a given model type.
 * 
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 * @param <T>
 */
public interface UserInterface <T> {

  /**
   * Called to tell the UserInterface to render itself based on the presented model.

   * @param model -> The object to use when rendering the interface
   * @return -> this interface, for fluent method chaining.
   */
  UserInterface<T> renderUi(T model);
  
  UserInterface<T> setRenderers(UiRenderingContext[] renderers);
}
