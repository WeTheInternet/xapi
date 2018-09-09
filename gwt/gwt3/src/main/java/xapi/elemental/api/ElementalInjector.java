package xapi.elemental.api;

import elemental2.core.Function;
import elemental2.dom.Node;
import javaemul.internal.JsUtils;
import jsinterop.base.Any;
import jsinterop.base.Js;
import xapi.ui.api.ElementInjector;
import xapi.ui.api.ElementPosition;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/24/18.
 */
public class ElementalInjector implements ElementInjector<Node> {

    protected static Function insertAdjacentNode = Js.uncheckedCast(
        JsUtils.getProperty(
            htmlElementPrototype(),
            "insertAdjacentElement"
        )
    );

    private static native Any htmlElementPrototype()
    /*-{
      return Object.create($wnd.HTMLElement.prototype);
    }-*/;

    private final Node self;

    public ElementalInjector(Node self) {
        this.self = self;
    }

    @Override
    public void appendChild(Node newChild) {
        self.appendChild(newChild);
    }

    @Override
    public void insertBefore(Node newChild, Node refChild) {
        self.insertBefore(newChild, refChild);
    }

    @Override
    public void insertAtBegin(Node newChild) {
        insertAdjacentNode.call(self, ElementPosition.AFTER_BEGIN.position(), newChild);
    }

    @Override
    public void insertAfter(Node newChild) {
        insertAdjacentNode.call(self, ElementPosition.AFTER_END.position(), newChild);
    }

    @Override
    public void insertAtEnd(Node newChild) {
        insertAdjacentNode.call(self, ElementPosition.BEFORE_END.position(), newChild);
    }

    @Override
    public void removeChild(Node child) {
        self.removeChild(child);
    }

    @Override
    public void replaceChild(Node newChild, Node refChild) {
        self.replaceChild(newChild, refChild);
    }

    @Override
    public Node getParent(Node child) {
        return child.parentNode;
    }
}
