package xapi.ui.api.component;

import xapi.fu.In1Out1;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/25/17.
 */
public class ComponentConstructor<E, C extends IsComponent<E>> {

    private final In1Out1<ComponentOptions<E, C>, E> factory;

    public ComponentConstructor(In1Out1<ComponentOptions<E, C>, E> factory) {
        this.factory = factory;
    }

    public E constructElement(ComponentOptions<E, C> opts) {
        return factory.io(opts);
    }

    public C constructComponent(
        ComponentOptions<E, C> opts,
        In1Out1<E, ? extends C> getUi
    ) {
        E element = constructElement(opts);
        return getUi.io(element);
    }


}
