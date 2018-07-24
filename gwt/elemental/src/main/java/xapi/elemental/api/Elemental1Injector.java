package xapi.elemental.api;

import elemental.dom.Node;
import elemental2.core.Function;
import javaemul.internal.JsUtils;
import jsinterop.base.Js;
import xapi.ui.api.ElementInjector;
import xapi.ui.api.ElementPosition;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/23/18.
 */
public class Elemental1Injector implements ElementInjector<Node> {

    protected static Function insertAdjacentNode = Js.uncheckedCast(
        JsUtils.getProperty(
            htmlElementPrototype(),
            "insertAdjacentElement")
    );

    private static native JavaScriptObject htmlElementPrototype()
    /*-{
      return Object.create($wnd.HTMLElement.prototype);
    }-*/;

    private final Node self;

    public Elemental1Injector(Node self) {
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
}
