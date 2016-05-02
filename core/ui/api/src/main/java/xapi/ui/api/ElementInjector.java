package xapi.ui.api;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/18/16.
 */
public interface ElementInjector {

  void appendChild(UiElement newChild);

  void insertBefore(UiElement newChild);

  void insertAtBegin(UiElement newChild);

  void insertAfter(UiElement newChild);

  void insertAtEnd(UiElement newChild);

  void removeChild(UiElement child);
}
