package xapi.components.api;

import xapi.fu.In1Out1;
import xapi.ui.api.component.ComponentOptions;
import xapi.ui.api.component.IsComponent;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/25/17.
 */
public class ComponentConstructor<E, C extends IsComponent<E, C>> {

    private final In1Out1<ComponentOptions<E, C>, E> factory;

    protected class JsAdapter implements In1Out1<ComponentOptions<E, C>, E> {

        private final JavaScriptObject ctor;

        protected JsAdapter(JavaScriptObject ctor){
            this.ctor = ctor;
        }

        @Override
        public native E io(ComponentOptions<E, C> opts)
        /*-{
            var make = this.@JsAdapter::ctor;
            return new make(opts);
        }-*/;
    }

    public ComponentConstructor(JavaScriptObject obj) {
        this.factory = adaptJso(obj);
    }

    protected In1Out1<ComponentOptions<E, C>,E> adaptJso(JavaScriptObject obj) {
        return new JsAdapter(obj);
    }

    public ComponentConstructor(In1Out1<ComponentOptions<E, C>, E> factory) {
        this.factory = factory;
    }

    public E construct(ComponentOptions<E, C> opts) {
        return factory.io(opts);
    }
}
