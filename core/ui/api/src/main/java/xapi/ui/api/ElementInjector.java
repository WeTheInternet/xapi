package xapi.ui.api;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/18/16.
 */
public interface ElementInjector
    <Element> {

  <El extends UiElement<Element, El>> void appendChild(El newChild);

  <El extends UiElement<Element, El>> void insertBefore(El newChild);

  <El extends UiElement<Element, El>> void insertAtBegin(El newChild);

  <El extends UiElement<Element, El>> void insertAfter(El newChild);

  <El extends UiElement<Element, El>> void insertAtEnd(El newChild);

  <El extends UiElement<Element, El>> void removeChild(El child);
}
