package xapi.ui.api.component;

import xapi.fu.In1Out1;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/25/17.
 */
public class ComponentConstructor<N, E extends N, C extends IsComponent<N, E>> {

    private final In1Out1<ComponentOptions<N, E, C>, E> factory;

    public ComponentConstructor(In1Out1<ComponentOptions<N, E, C>, E> factory) {
        this.factory = factory;
    }

    public E construct(ComponentOptions<N, E, C> opts) {
        return factory.io(opts);
    }
}
