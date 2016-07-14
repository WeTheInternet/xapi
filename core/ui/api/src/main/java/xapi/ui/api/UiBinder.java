package xapi.ui.api;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/18/16.
 */
public interface UiBinder <Source, Element, E extends UiElement<? super Element, Element, E>> {

  UiElement<? super Element, Element, E> bind(Source source);
}
