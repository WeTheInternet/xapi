package xapi.ui.api;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/18/16.
 */
public interface ElementInjector <Element, E extends UiElement<? extends Element, E>> {

  <El extends E> void appendChild(El newChild);

  <El extends E> void insertBefore(El newChild);

  <El extends E> void insertAtBegin(El newChild);

  <El extends E> void insertAfter(El newChild);

  <El extends E> void insertAtEnd(El newChild);

  <El extends E> void removeChild(El child);
}
