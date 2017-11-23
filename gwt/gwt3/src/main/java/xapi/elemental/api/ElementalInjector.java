package xapi.elemental.api;

import elemental2.core.Function;
import elemental2.dom.Element;
import elemental2.dom.Node;
import javaemul.internal.JsUtils;
import jsinterop.base.Js;
import xapi.ui.api.ElementInjector;
import xapi.ui.api.ElementPosition;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/18/17.
 */
public class ElementalInjector implements ElementInjector<Node, UiElementGwt<?>> {

    protected static Function insertAdjacentElement = Js.uncheckedCast(
        JsUtils.getProperty(
            xapi.elemental.api.UiElementGwt.htmlElementPrototype(),
            "insertAdjacentElement")
    );

    private final Element self;

    public ElementalInjector(Element self) {
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
        insertAdjacentElement.call(self, ElementPosition.AFTER_BEGIN.position(), newChild.getElement());
    }

    @Override
    public void insertAfter(UiElementGwt<?> newChild) {
        insertAdjacentElement.call(self, ElementPosition.AFTER_END.position(), newChild.getElement());
    }

    @Override
    public void insertAtEnd(UiElementGwt<?> newChild) {
        insertAdjacentElement.call(self, ElementPosition.BEFORE_END.position(), newChild.getElement());
    }

    @Override
    public void removeChild(UiElementGwt<?> child) {
        self.removeChild(child.getElement());
    }
}
