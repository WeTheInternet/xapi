package xapi.ui.api.component;

import xapi.fu.Immutable;
import xapi.fu.In1Out1;
import xapi.model.api.Model;

/**
 * Implementation of {@link ComponentOptions} which includes support for
 * passing along a typed model (using a ModelCache as the intermediate
 * for innerHTML constructed components)
 *
 * Created by James X. Nelson (james @wetheinter.net) on 5/25/17.
 */
public class ModelComponentOptions
    <N, E extends N, M extends Model, C extends IsModelComponent<N, E, M>>
   extends ComponentOptions<N, E, C> {

    protected M model;

    public ModelComponentOptions<N, E, M, C> withComponent(C component) {
        super.withComponent(component);
        return this;
    }

    public C newComponent(E element) {
        if (element == null) {
            throw new NullPointerException();
        }
        return component.io(element);
    }

    public ModelComponentOptions<N, E, M, C> withComponent(In1Out1<E, C> componentFactory) {
        super.withComponent(componentFactory);
        return this;
    }

    public M getModel() {
        return model;
    }

    public void setModel(M model) {
        this.model = model;
    }
}
