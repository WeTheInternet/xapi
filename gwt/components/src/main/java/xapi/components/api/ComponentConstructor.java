package xapi.components.api;

import xapi.fu.In1Out1;
import xapi.fu.X_Fu;
import xapi.ui.api.UiElement;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/25/17.
 */
public class ComponentConstructor<E> {

    private final In1Out1<Object[], E> factory;

    protected class JsAdapter implements In1Out1<Object[], E> {

        private final JavaScriptObject ctor;

        protected JsAdapter(JavaScriptObject ctor){
            this.ctor = ctor;
        }

        @Override
        public E io(Object[] in) {
            // Here is where we determine how to invoke our factory
            if (X_Fu.isEmpty(in)) {
                // no arguments.
            } else {
                // we have args; lets see what types they are.
                if (in.length == 1) {
                    if (in[0] instanceof UiElement) {
                        // When the arguments to create an element
                        // is a UiElement, then we want to be filling in that element;
                        // otherwise, we want to be creating a new element.
                    }
                }
            }
            return null;
        }
    }

    public ComponentConstructor(JavaScriptObject obj) {
        this.factory = adaptJso(obj);
    }

    protected In1Out1<Object[],E> adaptJso(JavaScriptObject obj) {
        return new JsAdapter(obj);
    }

    public ComponentConstructor(In1Out1<Object[], E> factory) {
        this.factory = factory;
    }

    public E construct(Object ... args) {
        return factory.io(args == null ? new Object[0] : args);
    }
}
