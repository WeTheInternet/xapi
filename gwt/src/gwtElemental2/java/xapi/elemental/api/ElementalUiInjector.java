package xapi.elemental.api;

import elemental2.dom.Element;
import elemental2.dom.Node;
import xapi.ui.api.ElementPosition;
import xapi.ui.api.UiInjector;

import static xapi.elemental.api.ElementalInjector.insertAdjacentNode;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/18/17.
 */
public class ElementalUiInjector implements UiInjector<Node, UiElementGwt<?>> {

    private final Element self;

    public ElementalUiInjector(Element self) {
        this.self = self;
    }

    @Override
    public void appendChild(UiElementGwt<?> newChild) {
        self.appendChild(newChild.getElement());
    }

    @Override
    public void insertBefore(UiElementGwt<?> newChild, UiElementGwt<?> refChild) {
        self.insertBefore(newChild.getElement(), refChild.getElement());
    }

    @Override
    public void insertAtBegin(UiElementGwt<?> newChild) {
        insertAdjacentNode.call(self, ElementPosition.AFTER_BEGIN.position(), newChild.getElement());
    }

    @Override
    public void insertAfter(UiElementGwt<?> newChild) {
        insertAdjacentNode.call(self, ElementPosition.AFTER_END.position(), newChild.getElement());
    }

    @Override
    public void insertAtEnd(UiElementGwt<?> newChild) {
        insertAdjacentNode.call(self, ElementPosition.BEFORE_END.position(), newChild.getElement());
    }

    @Override
    public void removeChild(UiElementGwt<?> child) {
        self.removeChild(child.getElement());
    }

    @Override
    public void replaceChild(UiElementGwt<?> newChild, UiElementGwt<?> refChild) {
        self.replaceChild(newChild.getElement(), refChild.getElement());
    }

    @Override
    public UiElementGwt<?> getParent(UiElementGwt<?> child) {
        return child.getParent();
    }
}
